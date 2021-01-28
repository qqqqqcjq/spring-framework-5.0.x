/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.context.annotation.DeferredImportSelector.Group;
import org.springframework.core.NestedIOException;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Parses a {@link Configuration} class definition, populating a collection of
 * {@link ConfigurationClass} objects (parsing a single Configuration class may result in
 * any number of ConfigurationClass objects because one Configuration class may imports
 * another using the {@link Import} annotation).
 *
 * <p>This class helps separate the concern of parsing the structure of a Configuration
 * class from the concern of registering BeanDefinition objects based on the content of
 * that model (with the exception of {@code @ComponentScan} annotations which need to be
 * registered immediately).
 *
 * <p>This ASM-based implementation avoids reflection and eager class loading in order to
 * interoperate effectively with lazy class loading in a Spring ApplicationContext.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @since 3.0
 * @see ConfigurationClassBeanDefinitionReader
 */
//递归处理 ： 会将配置类封装为ConfigurationClass类，然后进行处理
//如果当前bd没有@Configruation，但是有@Component @ConponentScan @Import @ImportResource其中的任何一个，或者类里面有@bean methbod,则spring认为它是一个半注解的类
//如果存在Configuration 注解,则为BeanDefinition 设置configurationClass属性为Full， 属于全注解类
//Spring会为全注解类创建一个代理类
//不管是全注解类还是半注解类， spirng都会交给ConfigurationClassParser迭代处理，所以@PropertySource等放在@CoConfiguration或者@ComponentScan注解的类上面都可以

//@Configuration类解析器
//解析内部类
//解析@PropertySource注解
//处理@ComponentScan注解
//检验获得的BeanDefinition中是否有配置类
//解析 @Import 注解
//解析 @ImportResource 注解
//解析@Bean方法
//如果有父类，则解析父类
class ConfigurationClassParser {

	private static final PropertySourceFactory DEFAULT_PROPERTY_SOURCE_FACTORY = new DefaultPropertySourceFactory();


	//使用这个比较器对ConfigurationClassParser.deferredImportSelectors进行排序
	private static final Comparator<DeferredImportSelectorHolder> DEFERRED_IMPORT_COMPARATOR =
			(o1, o2) -> AnnotationAwareOrderComparator.INSTANCE.compare(o1.getImportSelector(), o2.getImportSelector());


	private final Log logger = LogFactory.getLog(getClass());

	private final MetadataReaderFactory metadataReaderFactory;

	private final ProblemReporter problemReporter;

	private final Environment environment;

	private final ResourceLoader resourceLoader;

	private final BeanDefinitionRegistry registry;

	private final ComponentScanAnnotationParser componentScanParser;

	private final ConditionEvaluator conditionEvaluator;

    //一个map，用来存放ConfigurationClass
    //普通bean的 class都会被构造成ConfigurationClass，然后当做配置类处理一次(除了bd中beanfactoryname不为null的class,具体看checkConfigurationClassCandidate()方法)
    //特殊类中@Import引入的所有类会被构造成ConfigurationClass，然后当做配置类处理一次，其他@Bean  @ImportResource引入的bean不会被构造成ConfigurationClass，然后当做配置类处理一次
	private final Map<ConfigurationClass, ConfigurationClass> configurationClasses = new LinkedHashMap<>();

	private final Map<String, ConfigurationClass> knownSuperclasses = new HashMap<>();

	private final List<String> propertySourceNames = new ArrayList<>();

	//导入栈，处理@Import注解和内部类时，都会用到这个导入栈
    //使用方式都是，开始处理时调用push()方法，处理结束后在finally中调用pop()方法，主要就是使用这个栈特性
	private final ImportStack importStack = new ImportStack();

	@Nullable
    //DeferredImportSelectorHolder里面保存DeferredImportSelector的实现类的class, 已经对应的实例
	private List<DeferredImportSelectorHolder> deferredImportSelectors;

	/**
	 * Create a new {@link ConfigurationClassParser} instance that will be used
	 * to populate the set of configuration classes.
	 */
	public ConfigurationClassParser(MetadataReaderFactory metadataReaderFactory,
			ProblemReporter problemReporter, Environment environment, ResourceLoader resourceLoader,
			BeanNameGenerator componentScanBeanNameGenerator, BeanDefinitionRegistry registry) {

		this.metadataReaderFactory = metadataReaderFactory;
		this.problemReporter = problemReporter;
		this.environment = environment;
		this.resourceLoader = resourceLoader;
		this.registry = registry;
		this.componentScanParser = new ComponentScanAnnotationParser(
				environment, resourceLoader, componentScanBeanNameGenerator, registry);
		this.conditionEvaluator = new ConditionEvaluator(registry, environment, resourceLoader);
	}


	public void parse(Set<BeanDefinitionHolder> configCandidates) {
		this.deferredImportSelectors = new LinkedList<>();
		//根据BeanDefinition 的类型 做不同的处理,一般都会调用ConfigurationClassParser#parse 进行解析
		for (BeanDefinitionHolder holder : configCandidates) {
			BeanDefinition bd = holder.getBeanDefinition();
			try {
				if (bd instanceof AnnotatedBeanDefinition) {
					//解析注解对象，并且把getLazyResolutionProxyIfNecessary 解析出来的普通bd放到bdmap
					//另外解析@bean method/@ImportResource/ @Import /ImportBeanDefinitionRegistrar 填充configClasses,然后this.reader.loadBeanDefinitions(configClasses)再注册这些bd到bdmap
					parse(((AnnotatedBeanDefinition) bd).getMetadata(), holder.getBeanName());
				}
				else if (bd instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) bd).hasBeanClass()) {
					parse(((AbstractBeanDefinition) bd).getBeanClass(), holder.getBeanName());
				}
				else {
					parse(bd.getBeanClassName(), holder.getBeanName());
				}
			}
			catch (BeanDefinitionStoreException ex) {
				throw ex;
			}
			catch (Throwable ex) {
				throw new BeanDefinitionStoreException(
						"Failed to parse configuration class [" + bd.getBeanClassName() + "]", ex);
			}
		}

		// 处理DeferredImportSelector实现类，ImportSelector实现类在此之前已经处理
        // do{
        //     parse() ==> 扫描普通bean，得到bd到bdmap, 处理@Bean @Import @ImportResource等信息填充configclass属性
        //     processDeferredImportSelectors() ==> 填充configclass属性
        //     this.reader.loadBeanDefinitions(configClasses) 将当前configclass保存的@Bean @Import @ImportResource等信息对应的bean 的bd注册到bdmap
        // }while(配置类为空)
		processDeferredImportSelectors();
	}

	protected final void parse(@Nullable String className, String beanName) throws IOException {
		Assert.notNull(className, "No bean class name for configuration class bean definition");
		MetadataReader reader = this.metadataReaderFactory.getMetadataReader(className);
		processConfigurationClass(new ConfigurationClass(reader, beanName));
	}

	protected final void parse(Class<?> clazz, String beanName) throws IOException {
		processConfigurationClass(new ConfigurationClass(clazz, beanName));
	}

	protected final void parse(AnnotationMetadata metadata, String beanName) throws IOException {
		processConfigurationClass(new ConfigurationClass(metadata, beanName));
	}

	/**
	 * Validate each {@link ConfigurationClass} object.
	 * @see ConfigurationClass#validate
	 */
	public void validate() {
		for (ConfigurationClass configClass : this.configurationClasses.keySet()) {
			configClass.validate(this.problemReporter);
		}
	}

	public Set<ConfigurationClass> getConfigurationClasses() {
		return this.configurationClasses.keySet();
	}


	//这个方法是递归的，递归流程为:
    //parse()->processConfigurationClass(扫描出来的配置类封装为ConfigurationClass)->doProcessConfigurationClass()->parse()
	protected void processConfigurationClass(ConfigurationClass configClass) throws IOException {

	    //
		if (this.conditionEvaluator.shouldSkip(configClass.getMetadata(), ConfigurationPhase.PARSE_CONFIGURATION)) {
			return;
		}

		//判断当前配置类是否是否已经在配置类缓存中
		ConfigurationClass existingClass = this.configurationClasses.get(configClass);
		if (existingClass != null) {

            //判断当前的配置类是不是被@Import导入的配置类，如果是的话，直接返回
            //this.reader.loadBeanDefinitions(configClasses)方法里面处理@bean method引入的bean  @ImportResource引入的xml配置的bean ImportBeanDefinitionRegistrar引入的bean的bd的注册，  @import引入的配置类 的处理
            if (configClass.isImported()) {
				if (existingClass.isImported()) {
					existingClass.mergeImportedBy(configClass);
				}
				// Otherwise ignore new imported config class; existing non-imported class overrides it.
				return;
			}
			else {
				// Explicit bean definition found, probably replacing an imports.
				// Let's remove the old one and go with the new one.
                //删除缓存中的配置类，重新处理新的缓存类
				this.configurationClasses.remove(configClass);
				this.knownSuperclasses.values().removeIf(configClass::equals);
			}
		}

		// Recursively process the configuration class and its superclass hierarchy.
        // @Configuration注解定义中有@Inherited，所以@Configuration注解的类的子类也默认被@Configuration注解
		SourceClass sourceClass = asSourceClass(configClass);
		do {
			sourceClass = doProcessConfigurationClass(configClass, sourceClass);
		}
		while (sourceClass != null);
		//一个map，用来存放ConfigurationClass
        //普通bean的 class都会被构造成ConfigurationClass，然后当做配置类处理一次(除了bd中beanfactoryname不为null的class,具体看checkConfigurationClassCandidate()方法)
        //特殊类中@Import引入的所有类会被构造成ConfigurationClass，然后当做配置类处理一次，其他@Bean  @ImportResource引入的bean不会被构造成ConfigurationClass，然后当做配置类处理一次
		this.configurationClasses.put(configClass, configClass);
	}

	/**
	 * Apply processing and build a complete {@link ConfigurationClass} by reading the
	 * annotations, members and methods from the source class. This method can be called
	 * multiple times as relevant sources are discovered.
	 * @param configClass the configuration class being build
	 * @param sourceClass a source class
	 * @return the superclass, or {@code null} if none found or previously processed
	 */
	@Nullable
	protected final SourceClass doProcessConfigurationClass(ConfigurationClass configClass, SourceClass sourceClass)
			throws IOException {

		// Recursively process any member (nested) classes first
		// 处理内部类， 我们的config类里面可以定义内部类，就在这里处理，很少用，一般我们不会定义内部类
        // spring boot中的那些自动配置类里面都会有很多内部类，就是用这个方法处理
		processMemberClasses(configClass, sourceClass);

		// Process any @PropertySource annotations : 处理@PropertySource注解，解析.properties文件
        //最终保存在ConfigurationClassParser#environment==>AbstractEnvironment#propertySources中
		/*
		example:
		@Component
		@PropertySource(value = {"demo/props/demo.properties"})
		public class ReadByPropertySourceAndValue {
    		@Value("${demo.name}")
    		private String name;
		}
		 */
		for (AnnotationAttributes propertySource : AnnotationConfigUtils.attributesForRepeatable(
				sourceClass.getMetadata(), PropertySources.class,
				org.springframework.context.annotation.PropertySource.class)) {
			if (this.environment instanceof ConfigurableEnvironment) {
				processPropertySource(propertySource);
			}
			else {
				logger.warn("Ignoring @PropertySource annotation on [" + sourceClass.getMetadata().getClassName() +
						"]. Reason: Environment must implement ConfigurableEnvironment");
			}
		}

		// Process any @ComponentScan annotations
        // 处理@ComponentScan注解
		Set<AnnotationAttributes> componentScans = AnnotationConfigUtils.attributesForRepeatable(
				sourceClass.getMetadata(), ComponentScans.class, ComponentScan.class);
		if (!componentScans.isEmpty() &&
				!this.conditionEvaluator.shouldSkip(sourceClass.getMetadata(), ConfigurationPhase.REGISTER_BEAN)) {
			for (AnnotationAttributes componentScan : componentScans) {
				// The config class is annotated with @ComponentScan -> perform the scan immediately
				//扫描普通类componentScan=com.luban
				//这里扫描出来所有@Component
				//并且把扫描的出来的普通bean放到map当中
				Set<BeanDefinitionHolder> scannedBeanDefinitions =
						this.componentScanParser.parse(componentScan, sourceClass.getMetadata().getClassName());

				// Check the set of scanned definitions for any further config classes and parse recursively if needed
				// 检查扫描出来的bd会调用checkConfigurationClassCandidate()判断是否可以作为配置类，一般都需要作为配置类处理，具体判断条件看checkConfigurationClassCandidate()
                // 描出的每个bean都会判断是full还是lite,然后再递归处理
				for (BeanDefinitionHolder holder : scannedBeanDefinitions) {
					BeanDefinition bdCand = holder.getBeanDefinition().getOriginatingBeanDefinition();
					if (bdCand == null) {
						bdCand = holder.getBeanDefinition();
					}
					//检查给定的bean定义是否是配置类，full lite 并相应地进行标记。
                    //通过checkConfigurationClassCandidate()方法可以看出， 只有bd中AbstractBeanDefinition.factoryMethodName属性不为空才不可以作为配置类，其他bd 对应的class都需要作为配置类进行处理
                    //所以基本每一个类都会被当作配置类递归处理，经过测试，可以看出@Configuration注解的类或者@Component注解的类，里面都可以带有@Import @ImportResource @Bean都可以，@Configuration里面可以有的注解，@Component都可以拥有
                    //在扫描bd到bdmap阶段,@Configuration注解的类或者@Component注解的类作为配置类是完全一样的，我们只需要通过容器的构造函数参数传进去或者调用applicationContext.register(class)首先注册一个@Configuration注解的类或者@Component注解的类启动扫描过程就行(没有写错，手工注册的第一个bean被@Component或者@Configuration注解都可以，只要没有factorymethodname就好，启动扫描的过程是：1.从beanfactory拿出所有beanname, 调用checkConfigurationClassCandidate判断是不是一个配置类，是的话，启动递归处理)
                    //@Component和@Configuration注解的类的区别在于，@Configuration注解的类，会生成一个代理类，来服务@bean method引入的bean的实例化， 这个代理类可以做到：@Bean中每次new bean之前，先使用beanFactory获取一下，获取到了不创建，没有获取到再创建，这样就不会违反spring的singleton bean规则。但是@Component注解的类就没有这个功能，所以最好不要在@Component注解的类里面写@Bean
                    if (ConfigurationClassUtils.checkConfigurationClassCandidate(bdCand, this.metadataReaderFactory)) {
					    //处理扫描到的新的配置类 ，递归的处理
						parse(bdCand.getBeanClassName(), holder.getBeanName());
					}
				}
			}
		}

		/**
		 * 上面的代码就是扫描普通类----@Component
		 * 并且放到了map当中
		 */

		//=======================扫描普通类----@Component 并且放到了map当中 end=========================//

		// Process any @Import annotations
		//处理@Import  imports 3种情况
		//ImportSelector
		//普通类
		//ImportBeanDefinitionRegistrar
		//这里和内部递归调用时候的情况不同
		/**
		 * 这里处理的import是需要判断我们的类当中时候有@Import注解
		 * 如果有这把@Import当中的值拿出来，是一个类
		 * 比如@Import(xxxxx.class)，那么这里便把xxxxx传进去进行解析
		 * 在解析的过程中如果发觉是一个importSelector那么就回调selectImports的方法
		 * 返回一个字符串（类名），通过这个字符串得到一个类
		 * 继而在递归调用本方法来处理这个类
		 *
		 * 判断一组类是不是imports（3种import）
		 *
		 *
		 */
		processImports(configClass, sourceClass, getImports(sourceClass), true);

		// Process any @ImportResource annotations
        // 处理@ImportResource注解，这里只是将@ImportResource包含的信息填充ConfigurationClass.importedResources, 并没有把@ImportResource引入的xml中配置的bean的bd注册给bdmap
        // 为调用this.reader.loadBeanDefinitions(configClasses)准备：
        // this.reader.loadBeanDefinitions(configClasses)方法里面处理@bean method引入的bean  @ImportResource引入的xml配置的bean ImportBeanDefinitionRegistrar引入的bean的bd的注册，  @import引入的配置类 的处理
		AnnotationAttributes importResource =
				AnnotationConfigUtils.attributesFor(sourceClass.getMetadata(), ImportResource.class);
		if (importResource != null) {
			String[] resources = importResource.getStringArray("locations");
			Class<? extends BeanDefinitionReader> readerClass = importResource.getClass("reader");
			for (String resource : resources) {
				String resolvedResource = this.environment.resolveRequiredPlaceholders(resource);
				configClass.addImportedResource(resolvedResource, readerClass);
			}
		}

		// Process individual @Bean methods
        // 处理@bean method, 这里只是把@bean method信息封装成一个BeanMethod对象放进ConfigurationClass.beanMethods， 并没有注册bdmap
        // 为调用this.reader.loadBeanDefinitions(configClasses)准备：
        // this.reader.loadBeanDefinitions(configClasses)方法里面处理@bean method引入的bean  @ImportResource引入的xml配置的bean ImportBeanDefinitionRegistrar引入的bean的bd的注册，  @import引入的配置类 的处理
		Set<MethodMetadata> beanMethods = retrieveBeanMethodMetadata(sourceClass);
		for (MethodMetadata methodMetadata : beanMethods) {
			configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
		}

		// Process default methods on interfaces
		processInterfaces(configClass, sourceClass);

		// Process superclass, if any
		if (sourceClass.getMetadata().hasSuperClass()) {
			String superclass = sourceClass.getMetadata().getSuperClassName();
			if (superclass != null && !superclass.startsWith("java") &&
					!this.knownSuperclasses.containsKey(superclass)) {
				this.knownSuperclasses.put(superclass, configClass);
				// Superclass found, return its annotation metadata and recurse
				return sourceClass.getSuperClass();
			}
		}

		// No superclass -> processing is complete
		return null;
	}

	/**
	 * Register member (nested) classes that happen to be configuration classes themselves.
	 */
	private void processMemberClasses(ConfigurationClass configClass, SourceClass sourceClass) throws IOException {
		Collection<SourceClass> memberClasses = sourceClass.getMemberClasses();
		if (!memberClasses.isEmpty()) {
			List<SourceClass> candidates = new ArrayList<>(memberClasses.size());
			for (SourceClass memberClass : memberClasses) {
				if (ConfigurationClassUtils.isConfigurationCandidate(memberClass.getMetadata()) &&
						!memberClass.getMetadata().getClassName().equals(configClass.getMetadata().getClassName())) {
					candidates.add(memberClass);
				}
			}
			OrderComparator.sort(candidates);
			for (SourceClass candidate : candidates) {
				if (this.importStack.contains(configClass)) {
					this.problemReporter.error(new CircularImportProblem(configClass, this.importStack));
				}
				else {
					this.importStack.push(configClass);
					try {
						processConfigurationClass(candidate.asConfigClass(configClass));
					}
					finally {
						this.importStack.pop();
					}
				}
			}
		}
	}

	/**
	 * Register default methods on interfaces implemented by the configuration class.
	 */
	private void processInterfaces(ConfigurationClass configClass, SourceClass sourceClass) throws IOException {
		for (SourceClass ifc : sourceClass.getInterfaces()) {
			Set<MethodMetadata> beanMethods = retrieveBeanMethodMetadata(ifc);
			for (MethodMetadata methodMetadata : beanMethods) {
				if (!methodMetadata.isAbstract()) {
					// A default method or other concrete method on a Java 8+ interface...
					configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
				}
			}
			processInterfaces(configClass, ifc);
		}
	}

	/**
	 * Retrieve the metadata for all <code>@Bean</code> methods.
	 */
	private Set<MethodMetadata> retrieveBeanMethodMetadata(SourceClass sourceClass) {
		AnnotationMetadata original = sourceClass.getMetadata();
		Set<MethodMetadata> beanMethods = original.getAnnotatedMethods(Bean.class.getName());
		if (beanMethods.size() > 1 && original instanceof StandardAnnotationMetadata) {
			// Try reading the class file via ASM for deterministic declaration order...
			// Unfortunately, the JVM's standard reflection returns methods in arbitrary
			// order, even between different runs of the same application on the same JVM.
			try {
				AnnotationMetadata asm =
						this.metadataReaderFactory.getMetadataReader(original.getClassName()).getAnnotationMetadata();
				Set<MethodMetadata> asmMethods = asm.getAnnotatedMethods(Bean.class.getName());
				if (asmMethods.size() >= beanMethods.size()) {
					Set<MethodMetadata> selectedMethods = new LinkedHashSet<>(asmMethods.size());
					for (MethodMetadata asmMethod : asmMethods) {
						for (MethodMetadata beanMethod : beanMethods) {
							if (beanMethod.getMethodName().equals(asmMethod.getMethodName())) {
								selectedMethods.add(beanMethod);
								break;
							}
						}
					}
					if (selectedMethods.size() == beanMethods.size()) {
						// All reflection-detected methods found in ASM method set -> proceed
						beanMethods = selectedMethods;
					}
				}
			}
			catch (IOException ex) {
				logger.debug("Failed to read class file via ASM for determining @Bean method order", ex);
				// No worries, let's continue with the reflection metadata we started with...
			}
		}
		return beanMethods;
	}


	/**
	 * Process the given <code>@PropertySource</code> annotation metadata.
	 * @param propertySource metadata for the <code>@PropertySource</code> annotation found
	 * @throws IOException if loading a property source failed
	 */
    //解析@PropertySource后得到的PropertySource对象
    //最终保存在ConfigurationClassParser#environment==>AbstractEnvironment#propertySources中
	private void processPropertySource(AnnotationAttributes propertySource) throws IOException {
		String name = propertySource.getString("name");
		if (!StringUtils.hasLength(name)) {
			name = null;
		}
		String encoding = propertySource.getString("encoding");
		if (!StringUtils.hasLength(encoding)) {
			encoding = null;
		}
		String[] locations = propertySource.getStringArray("value");
		Assert.isTrue(locations.length > 0, "At least one @PropertySource(value) location is required");
		boolean ignoreResourceNotFound = propertySource.getBoolean("ignoreResourceNotFound");

		Class<? extends PropertySourceFactory> factoryClass = propertySource.getClass("factory");
		PropertySourceFactory factory = (factoryClass == PropertySourceFactory.class ?
				DEFAULT_PROPERTY_SOURCE_FACTORY : BeanUtils.instantiateClass(factoryClass));

		for (String location : locations) {
			try {
				String resolvedLocation = this.environment.resolveRequiredPlaceholders(location);
				Resource resource = this.resourceLoader.getResource(resolvedLocation);

				//保存ConfigurationClassPostProcessor.java解析@PropertySource后得到的PropertySource对象
				//最终保存在ConfigurationClassParser#environment==>AbstractEnvironment#propertySources中
				addPropertySource(factory.createPropertySource(name, new EncodedResource(resource, encoding)));
			}
			catch (IllegalArgumentException | FileNotFoundException | UnknownHostException ex) {
				// Placeholders not resolvable or resource not found when trying to open it
				if (ignoreResourceNotFound) {
					if (logger.isInfoEnabled()) {
						logger.info("Properties location [" + location + "] not resolvable: " + ex.getMessage());
					}
				}
				else {
					throw ex;
				}
			}
		}
	}

	private void addPropertySource(PropertySource<?> propertySource) {
		String name = propertySource.getName();
		MutablePropertySources propertySources = ((ConfigurableEnvironment) this.environment).getPropertySources();

		if (this.propertySourceNames.contains(name)) {
			// We've already added a version, we need to extend it
			PropertySource<?> existing = propertySources.get(name);
			if (existing != null) {
				PropertySource<?> newSource = (propertySource instanceof ResourcePropertySource ?
						((ResourcePropertySource) propertySource).withResourceName() : propertySource);
				if (existing instanceof CompositePropertySource) {
					((CompositePropertySource) existing).addFirstPropertySource(newSource);
				}
				else {
					if (existing instanceof ResourcePropertySource) {
						existing = ((ResourcePropertySource) existing).withResourceName();
					}
					CompositePropertySource composite = new CompositePropertySource(name);
					composite.addPropertySource(newSource);
					composite.addPropertySource(existing);
					propertySources.replace(name, composite);
				}
				return;
			}
		}

		if (this.propertySourceNames.isEmpty()) {
			propertySources.addLast(propertySource);
		}
		else {
			String firstProcessed = this.propertySourceNames.get(this.propertySourceNames.size() - 1);
			propertySources.addBefore(firstProcessed, propertySource);
		}
		this.propertySourceNames.add(name);
	}


	/**
	 * Returns {@code @Import} class, considering all meta-annotations.
     * 根据配置类的sourceClass的注解信息 返回所有的@Import引入的类的SourceClass
	 */
	private Set<SourceClass> getImports(SourceClass sourceClass) throws IOException {
		Set<SourceClass> imports = new LinkedHashSet<>();
		Set<SourceClass> visited = new LinkedHashSet<>();
		collectImports(sourceClass, imports, visited);
		return imports;
	}

	/**
	 * Recursively collect all declared {@code @Import} values. Unlike most
	 * meta-annotations it is valid to have several {@code @Import}s declared with
	 * different values; the usual process of returning values from the first
	 * meta-annotation on a class is not sufficient.
	 * <p>For example, it is common for a {@code @Configuration} class to declare direct
	 * {@code @Import}s in addition to meta-imports originating from an {@code @Enable}
	 * annotation.
	 * @param sourceClass the class to search
	 * @param imports the imports collected so far
	 * @param visited used to track visited classes to prevent infinite recursion
	 * @throws IOException if there is any problem reading metadata from the named class
	 */
	private void collectImports(SourceClass sourceClass, Set<SourceClass> imports, Set<SourceClass> visited)
			throws IOException {

		if (visited.add(sourceClass)) {
			for (SourceClass annotation : sourceClass.getAnnotations()) {
				String annName = annotation.getMetadata().getClassName();
				if (!annName.startsWith("java") && !annName.equals(Import.class.getName())) {
					//递归调用
					collectImports(annotation, imports, visited);
				}
			}
			imports.addAll(sourceClass.getAnnotationAttributes(Import.class.getName(), "value"));
		}
	}

	//处理@Import(DeferredImportSelector)中的DeferredImportSelector
	private void processDeferredImportSelectors() {
	    //将this.deferredImportSelectors赋值给deferredImports
		List<DeferredImportSelectorHolder> deferredImports = this.deferredImportSelectors;
		this.deferredImportSelectors = null;
		if (deferredImports == null) {
			return;
		}

		//对deferredImports进行排序
		deferredImports.sort(DEFERRED_IMPORT_COMPARATOR);

		//==================构造DeferredImportSelectorGrouping   begin===============================
		//deferredImports是一个list, 遍历每一个DeferredImportSelectorHolder
        //DeferredImportSelectorHolder里面封装着DeferredImportSelector，DeferredImportSelector里面封装着Group，我们需要使用Group和DeferredImportSelector构造DeferredImportSelectorGrouping
		Map<Object, DeferredImportSelectorGrouping> groupings = new LinkedHashMap<>();
		Map<AnnotationMetadata, ConfigurationClass> configurationClasses = new HashMap<>();
		for (DeferredImportSelectorHolder deferredImport : deferredImports) {

			Class<? extends Group> group = deferredImport.getImportSelector().getImportGroup();

			//java 8 Map接口中的函数式编程
            //default V computeIfAbsent(K key,Function<? super K, ? extends V> mappingFunction)
            //如果key对应的value是null或者没有对应value, 就使用传入的Function计算一个value, 然后调用map.put(key. value)
			DeferredImportSelectorGrouping grouping = groupings.computeIfAbsent(
					(group != null ? group : deferredImport),
					key -> new DeferredImportSelectorGrouping(createGroup(group)));

			grouping.add(deferredImport);
			configurationClasses.put(deferredImport.getConfigurationClass().getMetadata(),
					deferredImport.getConfigurationClass());
		}
        //==================构造DeferredImportSelectorGrouping   end===============================

        //==================使用DeferredImportSelectorGrouping得到需要引入的bean 名字, 并使用processImports()方法处理 begin ===================================
		//调用processImports，并没有注册bd到bdmap, 只是new 一个ConfigurationClass， 最终注册bd到bdmap的地方是this.reader.loadBeanDefinitions(configClasses)
		for (DeferredImportSelectorGrouping grouping : groupings.values()) {
		    //grouping.getImports()方法里面处理DeferredImportSelectors，获取需要引入的bean的信息，然后使用processImports处理每一个需要引入的bean的信息
			grouping.getImports().forEach(entry -> {
				ConfigurationClass configurationClass = configurationClasses.get(entry.getMetadata());
				try {
                    // @Import引入的类最终都会在这里重新new 一个ConfigurationClass(importedBy属性传的是正在处理的配置类)
                    // ，processConfigurationClass(candidate.asConfigClass(configClass))中会判断配置类ConfigurationClass是否有importedBy属性
                    // ，有的话会调用existingClass.mergeImportedBy(configClass)，合并这种有importedBy属性ConfigurationClass到 第一个配置的importedBy属性中
                    // ，后续再调用this.reader.loadBeanDefinitions(configClasses)才会最终注册这类ConfigurationClass的bd到bdmap

                    // 为调用this.reader.loadBeanDefinitions(configClasses)准备：
                    // this.reader.loadBeanDefinitions(configClasses)方法里面处理@bean method引入的bean  @ImportResource引入的xml配置的bean ImportBeanDefinitionRegistrar引入的bean的bd的注册，  @import引入的配置类 的处理
					processImports(configurationClass, asSourceClass(configurationClass),
							asSourceClasses(entry.getImportClassName()), false);
				}
				catch (BeanDefinitionStoreException ex) {
					throw ex;
				}
				catch (Throwable ex) {
					throw new BeanDefinitionStoreException(
							"Failed to process imports candidates for configuration class [" +
							configurationClass.getMetadata().getClassName() + "]", ex);
				}
			});
		}
        //==================使用DeferredImportSelectorGrouping得到需要引入的bean, 并使用processImports()方法处理 end ===================================
	}

	private Group createGroup(@Nullable Class<? extends Group> type) {
		Class<? extends Group> effectiveType = (type != null ? type : DefaultDeferredImportSelectorGroup.class);
		Group group = BeanUtils.instantiateClass(effectiveType);
		ParserStrategyUtils.invokeAwareMethods(group,
				ConfigurationClassParser.this.environment,
				ConfigurationClassParser.this.resourceLoader,
				ConfigurationClassParser.this.registry);
		return group;
	}

	//configClass  传入的配置类
    //currentSourceClass 传入的配置类的SourceClass
    //importCandidates 配置类的@import注解引入的类的SourceClass集合
    //checkForCircularImports 检查一个类是否被循环@import
	private void processImports(ConfigurationClass configClass, SourceClass currentSourceClass,
			Collection<SourceClass> importCandidates, boolean checkForCircularImports) {

		if (importCandidates.isEmpty()) {
			return;
		}

		if (checkForCircularImports && isChainedImportOnStack(configClass)) {
			this.problemReporter.error(new CircularImportProblem(configClass, this.importStack));
		}
		else {
			this.importStack.push(configClass);
			try {
				for (SourceClass candidate : importCandidates) {

				    //处理ImportSelector类
					if (candidate.isAssignable(ImportSelector.class)) {
						// Candidate class is an ImportSelector -> delegate to it to determine imports
						Class<?> candidateClass = candidate.loadClass();
						//反射实现一个对象
                        //ConfigurationClassParser.processImports实例化ImportSelector实现类，处理ImportSelector实现的Aware接口
                        //然后如果是ImportSelector的直接实现类，注册里面需要引入的bd
                        //如果是 DeferredImportSelector实现类，DeferredImportSelectorHolder封装DeferredImportSelector实例和class，然后放进ConfigurationClassParser#deferredImportSelectors，parse()方法执行完后,
                        //再使用processDeferredImportSelectors()方法注册bd
						ImportSelector selector = BeanUtils.instantiateClass(candidateClass, ImportSelector.class);
						ParserStrategyUtils.invokeAwareMethods(
								selector, this.environment, this.resourceLoader, this.registry);
						if (this.deferredImportSelectors != null && selector instanceof DeferredImportSelector) {
							this.deferredImportSelectors.add(
									new DeferredImportSelectorHolder(configClass, (DeferredImportSelector) selector));
						}
						else {
							//回调， selector.selectImports(currentSourceClass.getMetadata()) 参数是当前类的annotationmetadata
							String[] importClassNames = selector.selectImports(currentSourceClass.getMetadata());
							Collection<SourceClass> importSourceClasses = asSourceClasses(importClassNames);
							//递归，这里第二次调用processImports
							//如果是一个普通类，会进else
							processImports(configClass, currentSourceClass, importSourceClasses, false);
						}
					}
                    //处理ImportBeanDefinitionRegistrar类
					else if (candidate.isAssignable(ImportBeanDefinitionRegistrar.class)) {
						// Candidate class is an ImportBeanDefinitionRegistrar ->
						// delegate to it to register additional bean definitions
						Class<?> candidateClass = candidate.loadClass();
						ImportBeanDefinitionRegistrar registrar =
								BeanUtils.instantiateClass(candidateClass, ImportBeanDefinitionRegistrar.class);
						ParserStrategyUtils.invokeAwareMethods(
								registrar, this.environment, this.resourceLoader, this.registry);
						// 将@Import(ImportBeanDefinitionRegistrar) 中得到的ImportBeanDefinitionRegistrar添加到ConfigurationClass.importBeanDefinitionRegistrars，并没有注册ImportBeanDefinitionRegistrar引入的bean的bd到bdmap
                        // 为调用this.reader.loadBeanDefinitions(configClasses)准备：
                        // this.reader.loadBeanDefinitions(configClasses)方法里面处理@bean method引入的bean  @ImportResource引入的xml配置的bean ImportBeanDefinitionRegistrar引入的bean的bd的注册，  @import引入的配置类 的处理
						configClass.addImportBeanDefinitionRegistrar(registrar, currentSourceClass.getMetadata());
					}
					//处理@Configuration注解的配置类
                    //Candidate class不是ImportSelector， 也不是ImportBeanDefinitionRegistrar， 就把Candidate class当作@Configuration class来处理
					else {
						// Candidate class not an ImportSelector or ImportBeanDefinitionRegistrar ->
						// process it as an @Configuration class
                        // 在ConfigurationClassParser.ImportStack.imports保存  当前配置类的注解元数据 和 当前配置类的@import注解引入的类  之间的映射
                        // 一个应用就是ImportAware
						this.importStack.registerImport(
								currentSourceClass.getMetadata(), candidate.getMetadata().getClassName());
						//cjq2020-03-15注释 ： 这里由递归调用processConfigurationClass，processImports方法其实就是processConfigurationClass方法调用来的
                        //@import引入的类会被转换给@Configuration class, 在这里完成bd的注册
                        //虽然会被转化为@Configuration class，但是因为不会有@import @bean等，所以这些相关的分支都不会走
                        //只会调用registerBeanDefinitionForImportedConfigurationClass(configClass)完成bd的注册

                        // @Import引入的类最终都会在这里重新new 一个ConfigurationClass(importedBy属性传的是正在处理的配置类)
                        // ，processConfigurationClass(candidate.asConfigClass(configClass))中会判断配置类ConfigurationClass是否有importedBy属性
                        // ，有的话会调用existingClass.mergeImportedBy(configClass)，合并这种有importedBy属性ConfigurationClass到 第一个配置的importedBy属性中
                        // ，后续再调用this.reader.loadBeanDefinitions(configClasses)才会最终注册这类ConfigurationClass的bd到bdmap

                        // 为调用this.reader.loadBeanDefinitions(configClasses)准备：
                        // this.reader.loadBeanDefinitions(configClasses)方法里面处理@bean method引入的bean  @ImportResource引入的xml配置的bean ImportBeanDefinitionRegistrar引入的bean的bd的注册，  @import引入的配置类 的处理
                        processConfigurationClass(candidate.asConfigClass(configClass));
					}
				}
			}
			catch (BeanDefinitionStoreException ex) {
				throw ex;
			}
			catch (Throwable ex) {
				throw new BeanDefinitionStoreException(
						"Failed to process imports candidates for configuration class [" +
						configClass.getMetadata().getClassName() + "]", ex);
			}
			finally {
				this.importStack.pop();
			}
		}
	}

	private boolean isChainedImportOnStack(ConfigurationClass configClass) {
		if (this.importStack.contains(configClass)) {
			String configClassName = configClass.getMetadata().getClassName();
			AnnotationMetadata importingClass = this.importStack.getImportingClassFor(configClassName);
			while (importingClass != null) {
				if (configClassName.equals(importingClass.getClassName())) {
					return true;
				}
				importingClass = this.importStack.getImportingClassFor(importingClass.getClassName());
			}
		}
		return false;
	}

	ImportRegistry getImportRegistry() {
		return this.importStack;
	}


	/**
	 * Factory method to obtain a {@link SourceClass} from a {@link ConfigurationClass}.
	 */
	private SourceClass asSourceClass(ConfigurationClass configurationClass) throws IOException {
		AnnotationMetadata metadata = configurationClass.getMetadata();
		if (metadata instanceof StandardAnnotationMetadata) {
			return asSourceClass(((StandardAnnotationMetadata) metadata).getIntrospectedClass());
		}
		return asSourceClass(metadata.getClassName());
	}

	/**
	 * Factory method to obtain a {@link SourceClass} from a {@link Class}.
	 */
	SourceClass asSourceClass(@Nullable Class<?> classType) throws IOException {
		if (classType == null) {
			return new SourceClass(Object.class);
		}
		try {
			// Sanity test that we can reflectively read annotations,
			// including Class attributes; if not -> fall back to ASM
			for (Annotation ann : classType.getAnnotations()) {
				AnnotationUtils.validateAnnotation(ann);
			}
			return new SourceClass(classType);
		}
		catch (Throwable ex) {
			// Enforce ASM via class name resolution
			return asSourceClass(classType.getName());
		}
	}

	/**
	 * Factory method to obtain {@link SourceClass SourceClasss} from class names.
	 */
	private Collection<SourceClass> asSourceClasses(String... classNames) throws IOException {
		List<SourceClass> annotatedClasses = new ArrayList<>(classNames.length);
		for (String className : classNames) {
			annotatedClasses.add(asSourceClass(className));
		}
		return annotatedClasses;
	}

	/**
	 * Factory method to obtain a {@link SourceClass} from a class name.
	 */
	SourceClass asSourceClass(@Nullable String className) throws IOException {
		if (className == null) {
			return new SourceClass(Object.class);
		}
		if (className.startsWith("java")) {
			// Never use ASM for core java types
			try {
				return new SourceClass(ClassUtils.forName(className, this.resourceLoader.getClassLoader()));
			}
			catch (ClassNotFoundException ex) {
				throw new NestedIOException("Failed to load class [" + className + "]", ex);
			}
		}
		return new SourceClass(this.metadataReaderFactory.getMetadataReader(className));
	}


	@SuppressWarnings("serial")
    //ImportStack是一个ArrayDeque，里面保存了遇到的@Configuration类，保存的目的是防止类的重复import
    //ImportStack自己有一个imports属性，用来保存
	private static class ImportStack extends ArrayDeque<ConfigurationClass> implements ImportRegistry {

		private final MultiValueMap<String, AnnotationMetadata> imports = new LinkedMultiValueMap<>();

		public void registerImport(AnnotationMetadata importingClass, String importedClass) {
			this.imports.add(importedClass, importingClass);
		}

		@Override
		@Nullable
		public AnnotationMetadata getImportingClassFor(String importedClass) {
			return CollectionUtils.lastElement(this.imports.get(importedClass));
		}

		@Override
		public void removeImportingClass(String importingClass) {
			for (List<AnnotationMetadata> list : this.imports.values()) {
				for (Iterator<AnnotationMetadata> iterator = list.iterator(); iterator.hasNext();) {
					if (iterator.next().getClassName().equals(importingClass)) {
						iterator.remove();
						break;
					}
				}
			}
		}

		/**
		 * Given a stack containing (in order)
		 * <ul>
		 * <li>com.acme.Foo</li>
		 * <li>com.acme.Bar</li>
		 * <li>com.acme.Baz</li>
		 * </ul>
		 * return "[Foo->Bar->Baz]".
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder("[");
			Iterator<ConfigurationClass> iterator = iterator();
			while (iterator.hasNext()) {
				builder.append(iterator.next().getSimpleName());
				if (iterator.hasNext()) {
					builder.append("->");
				}
			}
			return builder.append(']').toString();
		}
	}


	//封装了当前正在处理的配置类和一个DeferredImportSelector
	private static class DeferredImportSelectorHolder {

		private final ConfigurationClass configurationClass;

		private final DeferredImportSelector importSelector;

		public DeferredImportSelectorHolder(ConfigurationClass configClass, DeferredImportSelector selector) {
			this.configurationClass = configClass;
			this.importSelector = selector;
		}

		public ConfigurationClass getConfigurationClass() {
			return this.configurationClass;
		}

		public DeferredImportSelector getImportSelector() {
			return this.importSelector;
		}
	}


	//封装了Group(spring framework中只有一个实现类DefaultDeferredImportSelectorGroup)和所有的DeferredImportSelectorHolder
    //定义了一个getImports()方法作为入口，使用Group.process()方法循环处理DeferredImportSelectorHolder， 最终需要引入的bean信息会被构造成Entry放在Group中的List<Entry> imports
	private static class DeferredImportSelectorGrouping {

		private final DeferredImportSelector.Group group;

		private final List<DeferredImportSelectorHolder> deferredImports = new ArrayList<>();

		DeferredImportSelectorGrouping(Group group) {
			this.group = group;
		}

		public void add(DeferredImportSelectorHolder deferredImport) {
			this.deferredImports.add(deferredImport);
		}

		/**
		 * Return the imports defined by the group.
		 * @return each imports with its associated configuration class
		 */
		//返回所有DeferredImportSelector引入的bean的集合
		public Iterable<Group.Entry> getImports() {
			for (DeferredImportSelectorHolder deferredImport : this.deferredImports) {
			    //调用group的process处理DeferredImportSelectorGrouping.deferredImports中保存的每一个DeferredImportSelectorHolder， 获取要注入的bean 信息保存到group中
				this.group.process(deferredImport.getConfigurationClass().getMetadata(),
						deferredImport.getImportSelector());
			}
			//调用group.selectImports获取group中保存的所有需要注入的bean的信息
			return this.group.selectImports();
		}
	}


	//ConfigurationClassParser.DefaultDeferredImportSelectorGroup.imports里面包含多个DeferredImportSelector.Group.Entry
    //每一个DeferredImportSelector.Group.Entry都封装一个selectImports得到的全类名
	private static class DefaultDeferredImportSelectorGroup implements Group {

		private final List<Entry> imports = new ArrayList<>();

		@Override
        //调用selectImports得到的每一个全类名，被封装为DeferredImportSelector.Group.Entry，然后添加到DefaultDeferredImportSelectorGroup.imports中，是一个list(可迭代的)
		public void process(AnnotationMetadata metadata, DeferredImportSelector selector) {
			for (String importClassName : selector.selectImports(metadata)) {
				this.imports.add(new Entry(metadata, importClassName));
			}
		}

		@Override
        //可迭代接口Iterable里面定义了获取迭代器的方法
		public Iterable<Entry> selectImports() {
			return this.imports;
		}
	}


	/**
     * 带有注释的类的包装器
	 * Simple wrapper that allows annotated source classes to be dealt with
	 * in a uniform manner, regardless of how they are loaded.
	 */
	private class SourceClass implements Ordered {

		private final Object source;  // Class or MetadataReader

		private final AnnotationMetadata metadata;

		public SourceClass(Object source) {
			this.source = source;
			if (source instanceof Class) {
				this.metadata = new StandardAnnotationMetadata((Class<?>) source, true);
			}
			else {
				this.metadata = ((MetadataReader) source).getAnnotationMetadata();
			}
		}

		public final AnnotationMetadata getMetadata() {
			return this.metadata;
		}

		@Override
		public int getOrder() {
			Integer order = ConfigurationClassUtils.getOrder(this.metadata);
			return (order != null ? order : Ordered.LOWEST_PRECEDENCE);
		}

		public Class<?> loadClass() throws ClassNotFoundException {
			if (this.source instanceof Class) {
				return (Class<?>) this.source;
			}
			String className = ((MetadataReader) this.source).getClassMetadata().getClassName();
			return ClassUtils.forName(className, resourceLoader.getClassLoader());
		}

		public boolean isAssignable(Class<?> clazz) throws IOException {
			if (this.source instanceof Class) {
				return clazz.isAssignableFrom((Class<?>) this.source);
			}
			return new AssignableTypeFilter(clazz).match((MetadataReader) this.source, metadataReaderFactory);
		}

		//将当前的source class 转换为 ConfigurationClass
		public ConfigurationClass asConfigClass(ConfigurationClass importedBy) throws IOException {
			if (this.source instanceof Class) {
				return new ConfigurationClass((Class<?>) this.source, importedBy);
			}
			return new ConfigurationClass((MetadataReader) this.source, importedBy);
		}

		public Collection<SourceClass> getMemberClasses() throws IOException {
			Object sourceToProcess = this.source;
			if (sourceToProcess instanceof Class) {
				Class<?> sourceClass = (Class<?>) sourceToProcess;
				try {
					Class<?>[] declaredClasses = sourceClass.getDeclaredClasses();
					List<SourceClass> members = new ArrayList<>(declaredClasses.length);
					for (Class<?> declaredClass : declaredClasses) {
						members.add(asSourceClass(declaredClass));
					}
					return members;
				}
				catch (NoClassDefFoundError err) {
					// getDeclaredClasses() failed because of non-resolvable dependencies
					// -> fall back to ASM below
					sourceToProcess = metadataReaderFactory.getMetadataReader(sourceClass.getName());
				}
			}

			// ASM-based resolution - safe for non-resolvable classes as well
			MetadataReader sourceReader = (MetadataReader) sourceToProcess;
			String[] memberClassNames = sourceReader.getClassMetadata().getMemberClassNames();
			List<SourceClass> members = new ArrayList<>(memberClassNames.length);
			for (String memberClassName : memberClassNames) {
				try {
					members.add(asSourceClass(memberClassName));
				}
				catch (IOException ex) {
					// Let's skip it if it's not resolvable - we're just looking for candidates
					if (logger.isDebugEnabled()) {
						logger.debug("Failed to resolve member class [" + memberClassName +
								"] - not considering it as a configuration class candidate");
					}
				}
			}
			return members;
		}

		public SourceClass getSuperClass() throws IOException {
			if (this.source instanceof Class) {
				return asSourceClass(((Class<?>) this.source).getSuperclass());
			}
			return asSourceClass(((MetadataReader) this.source).getClassMetadata().getSuperClassName());
		}

		public Set<SourceClass> getInterfaces() throws IOException {
			Set<SourceClass> result = new LinkedHashSet<>();
			if (this.source instanceof Class) {
				Class<?> sourceClass = (Class<?>) this.source;
				for (Class<?> ifcClass : sourceClass.getInterfaces()) {
					result.add(asSourceClass(ifcClass));
				}
			}
			else {
				for (String className : this.metadata.getInterfaceNames()) {
					result.add(asSourceClass(className));
				}
			}
			return result;
		}

		public Set<SourceClass> getAnnotations() throws IOException {
			Set<SourceClass> result = new LinkedHashSet<>();
			for (String className : this.metadata.getAnnotationTypes()) {
				try {
					result.add(getRelated(className));
				}
				catch (Throwable ex) {
					// An annotation not present on the classpath is being ignored
					// by the JVM's class loading -> ignore here as well.
				}
			}
			return result;
		}

		public Collection<SourceClass> getAnnotationAttributes(String annType, String attribute) throws IOException {
			Map<String, Object> annotationAttributes = this.metadata.getAnnotationAttributes(annType, true);
			if (annotationAttributes == null || !annotationAttributes.containsKey(attribute)) {
				return Collections.emptySet();
			}
			String[] classNames = (String[]) annotationAttributes.get(attribute);
			Set<SourceClass> result = new LinkedHashSet<>();
			for (String className : classNames) {
				result.add(getRelated(className));
			}
			return result;
		}

		private SourceClass getRelated(String className) throws IOException {
			if (this.source instanceof Class) {
				try {
					Class<?> clazz = ClassUtils.forName(className, ((Class<?>) this.source).getClassLoader());
					return asSourceClass(clazz);
				}
				catch (ClassNotFoundException ex) {
					// Ignore -> fall back to ASM next, except for core java types.
					if (className.startsWith("java")) {
						throw new NestedIOException("Failed to load class [" + className + "]", ex);
					}
					return new SourceClass(metadataReaderFactory.getMetadataReader(className));
				}
			}
			return asSourceClass(className);
		}

		@Override
		public boolean equals(Object other) {
			return (this == other || (other instanceof SourceClass &&
					this.metadata.getClassName().equals(((SourceClass) other).metadata.getClassName())));
		}

		@Override
		public int hashCode() {
			return this.metadata.getClassName().hashCode();
		}

		@Override
		public String toString() {
			return this.metadata.getClassName();
		}
	}


	/**
	 * {@link Problem} registered upon detection of a circular {@link Import}.
	 */
	private static class CircularImportProblem extends Problem {

		public CircularImportProblem(ConfigurationClass attemptedImport, Deque<ConfigurationClass> importStack) {
			super(String.format("A circular @Import has been detected: " +
					"Illegal attempt by @Configuration class '%s' to imports class '%s' as '%s' is " +
					"already present in the current imports stack %s", importStack.element().getSimpleName(),
					attemptedImport.getSimpleName(), attemptedImport.getSimpleName(), importStack),
					new Location(importStack.element().getResource(), attemptedImport.getMetadata()));
		}
	}

}

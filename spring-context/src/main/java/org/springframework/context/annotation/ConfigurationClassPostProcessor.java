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

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.parsing.PassThroughSourceExtractor;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ConfigurationClassEnhancer.EnhancedConfiguration;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import static org.springframework.context.annotation.AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR;

/**
 * ConfigurationClassPostProcessor从名字就可以看出，他是处理配置类的postprocessor ,负责扫描、解析注解、import
 * {@link BeanFactoryPostProcessor} used for bootstrapping processing of
 * {@link Configuration @Configuration} classes.
 *
 * <p>Registered by default when using {@code <context:annotation-config/>} or
 * {@code <context:component-scan/>}. Otherwise, may be declared manually as
 * with any other BeanFactoryPostProcessor.
 *
 * <p>This post processor is priority-ordered as it is important that any
 * {@link Bean} methods declared in {@code @Configuration} classes have
 * their corresponding bean definitions registered before any other
 * {@link BeanFactoryPostProcessor} executes.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 3.0
 */
public class ConfigurationClassPostProcessor implements BeanDefinitionRegistryPostProcessor,
		PriorityOrdered, ResourceLoaderAware, BeanClassLoaderAware, EnvironmentAware {

	private static final String IMPORT_REGISTRY_BEAN_NAME =
			ConfigurationClassPostProcessor.class.getName() + ".importRegistry";


	private final Log logger = LogFactory.getLog(getClass());

	private SourceExtractor sourceExtractor = new PassThroughSourceExtractor();

	private ProblemReporter problemReporter = new FailFastProblemReporter();

	@Nullable
	private Environment environment;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();

	private boolean setMetadataReaderFactoryCalled = false;

	private final Set<Integer> registriesPostProcessed = new HashSet<>();

	private final Set<Integer> factoriesPostProcessed = new HashSet<>();

	@Nullable
	private ConfigurationClassBeanDefinitionReader reader;

	private boolean localBeanNameGeneratorSet = false;

	/* Using short class names as default bean names */
	private BeanNameGenerator componentScanBeanNameGenerator = new AnnotationBeanNameGenerator();

	/* Using fully qualified class names as default bean names */
	private BeanNameGenerator importBeanNameGenerator = new AnnotationBeanNameGenerator() {
		@Override
		protected String buildDefaultBeanName(BeanDefinition definition) {
			String beanClassName = definition.getBeanClassName();
			Assert.state(beanClassName != null, "No bean class name set");
			return beanClassName;
		}
	};


	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;  // within PriorityOrdered
	}

	/**
	 * Set the {@link SourceExtractor} to use for generated bean definitions
	 * that correspond to {@link Bean} factory methods.
	 */
	public void setSourceExtractor(@Nullable SourceExtractor sourceExtractor) {
		this.sourceExtractor = (sourceExtractor != null ? sourceExtractor : new PassThroughSourceExtractor());
	}

	/**
	 * Set the {@link ProblemReporter} to use.
	 * <p>Used to register any problems detected with {@link Configuration} or {@link Bean}
	 * declarations. For instance, an @Bean method marked as {@code final} is illegal
	 * and would be reported as a problem. Defaults to {@link FailFastProblemReporter}.
	 */
	public void setProblemReporter(@Nullable ProblemReporter problemReporter) {
		this.problemReporter = (problemReporter != null ? problemReporter : new FailFastProblemReporter());
	}

	/**
	 * Set the {@link MetadataReaderFactory} to use.
	 * <p>Default is a {@link CachingMetadataReaderFactory} for the specified
	 * {@linkplain #setBeanClassLoader bean class loader}.
	 */
	public void setMetadataReaderFactory(MetadataReaderFactory metadataReaderFactory) {
		Assert.notNull(metadataReaderFactory, "MetadataReaderFactory must not be null");
		this.metadataReaderFactory = metadataReaderFactory;
		this.setMetadataReaderFactoryCalled = true;
	}

	/**
	 * Set the {@link BeanNameGenerator} to be used when triggering component scanning
	 * from {@link Configuration} classes and when registering {@link Import}'ed
	 * configuration classes. The default is a standard {@link AnnotationBeanNameGenerator}
	 * for scanned components (compatible with the default in {@link ClassPathBeanDefinitionScanner})
	 * and a variant thereof for imported configuration classes (using unique fully-qualified
	 * class names instead of standard component overriding).
	 * <p>Note that this strategy does <em>not</em> apply to {@link Bean} methods.
	 * <p>This setter is typically only appropriate when configuring the post-processor as
	 * a standalone bean definition in XML, e.g. not using the dedicated
	 * {@code AnnotationConfig*} application contexts or the {@code
	 * <context:annotation-config>} element. Any bean name generator specified against
	 * the application context will take precedence over any value set here.
	 * @since 3.1.1
	 * @see AnnotationConfigApplicationContext#setBeanNameGenerator(BeanNameGenerator)
	 * @see AnnotationConfigUtils#CONFIGURATION_BEAN_NAME_GENERATOR
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		Assert.notNull(beanNameGenerator, "BeanNameGenerator must not be null");
		this.localBeanNameGeneratorSet = true;
		this.componentScanBeanNameGenerator = beanNameGenerator;
		this.importBeanNameGenerator = beanNameGenerator;
	}

	@Override
	public void setEnvironment(Environment environment) {
		Assert.notNull(environment, "Environment must not be null");
		this.environment = environment;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.resourceLoader = resourceLoader;
		if (!this.setMetadataReaderFactoryCalled) {
			this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
		}
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
		if (!this.setMetadataReaderFactoryCalled) {
			this.metadataReaderFactory = new CachingMetadataReaderFactory(beanClassLoader);
		}
	}


	/**
	 * Derive further bean definitions from the configuration classes in the registry.
	 */
	@Override
    //refresh();
    //==>invokeBeanFactoryPostProcessors(beanFactory);
    //==>PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());
    //==>invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
    //==>postProcessor.postProcessBeanDefinitionRegistry(registry); 就是下面这个方法了
    //接着调用这个方法里面的processConfigBeanDefinitions(registry);完成扫描将bd注册到bdmap
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
		int registryId = System.identityHashCode(registry);
		if (this.registriesPostProcessed.contains(registryId)) {
			throw new IllegalStateException(
					"postProcessBeanDefinitionRegistry already called on this post-processor against " + registry);
		}
		if (this.factoriesPostProcessed.contains(registryId)) {
			throw new IllegalStateException(
					"postProcessBeanFactory already called on this post-processor against " + registry);
		}
		this.registriesPostProcessed.add(registryId);

		processConfigBeanDefinitions(registry);
	}

	/**
	 * Prepare the Configuration classes for servicing bean requests at runtime
	 * by replacing them with CGLIB-enhanced subclasses.
	 */
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		int factoryId = System.identityHashCode(beanFactory);
		if (this.factoriesPostProcessed.contains(factoryId)) {
			throw new IllegalStateException(
					"postProcessBeanFactory already called on this post-processor against " + beanFactory);
		}
		this.factoriesPostProcessed.add(factoryId);
		if (!this.registriesPostProcessed.contains(factoryId)) {
			// BeanDefinitionRegistryPostProcessor hook apparently not supported...
			// Simply call processConfigurationClasses lazily at this point then.
			processConfigBeanDefinitions((BeanDefinitionRegistry) beanFactory);
		}
		/*
		给配置类产生cglib代理,为什么需要产生cglib代理？
		如果不加@configruation，那么就是使用原生的配置类
        如果加了@configruation，那么在enhanceConfigurationClasses(beanFactory)使用cglib给配置类生成一个代理类，
        这个代理类是配置类的子类，并且会设置继承BeanFactoryAware接口，这样代理类就可以使用BeanFactory,
        然后在@Bean中每次new bean之前，先使用beanFactory获取一下，获取到了不创建，没有获取到再创建，这样就不会违反spring的singleton bean规则
		*/
		enhanceConfigurationClasses(beanFactory);

		/*
		(bean的生命周期直说创建销毁太笼统， 可以更细分：扫描创建bd  bd到bdmap  实例化  初始化 穿插在这些过程中间的一些特殊处理，按照这个流程讲下来面试必过)
		InstantiationAwareBeanPostProcessor代表了Spring的另外一段生命周期：实例化。先区别一下Spring Bean的实例化和初始化两个阶段的主要作用：
        1、实例化----实例化的过程是一个创建Bean的过程，即调用Bean的构造函数，单例的Bean放入单例池中
        2、初始化----初始化的过程是一个赋值的过程，即调用Bean的setter，设置Bean的属性
        之前的BeanPostProcessor作用于过程（2）前后，现在的InstantiationAwareBeanPostProcessor则作用于过程（1）前后

		InstantiationAwareBeanPostProcessor接口继承BeanPostProcessor接口，它内部提供了3个方法，再加上BeanPostProcessor接口内部的2个方法，所以实现这个接口需要实现5个方法。InstantiationAwareBeanPostProcessor接口的主要作用在于目标对象的实例化过程中需要处理的事情，包括实例化对象的前后过程以及实例的属性设置
        postProcessBeforeInstantiation方法是最先执行的方法，它在目标对象实例化之前调用，该方法的返回值类型是Object，我们可以返回任何类型的值。由于这个时候目标对象还未实例化，所以这个返回值可以用来代替原本该生成的目标对象的实例(比如代理对象)。如果该方法的返回值代替原本该生成的目标对象，后续只有postProcessAfterInitialization方法会调用，其它方法不再调用；否则按照正常的流程走
        postProcessAfterInstantiation方法在目标对象实例化之后调用，这个时候对象已经被实例化，但是该实例的属性还未被设置，都是null。因为它的返回值是决定要不要调用postProcessPropertyValues方法的其中一个因素（因为还有一个因素是mbd.getDependencyCheck()）；如果该方法返回false,并且不需要check，那么postProcessPropertyValues就会被忽略不执行；如果返回true，postProcessPropertyValues就会被执行
        postProcessPropertyValues方法对属性值进行修改(这个时候属性值还未被设置，但是我们可以修改原本该设置进去的属性值)。如果postProcessAfterInstantiation方法返回false，该方法可能不会被调用。可以在该方法内对属性值进行修改
        父接口BeanPostProcessor的2个方法postProcessBeforeInitialization和postProcessAfterInitialization都是在目标对象被实例化之后，并且属性也被设置之后调用的
		 */
		beanFactory.addBeanPostProcessor(new ImportAwareBeanPostProcessor(beanFactory));
	}

	/**
	 * Build and validate a configuration model based on the registry of
	 * {@link Configuration} classes.
	 */
    //通过checkConfigurationClassCandidate()方法可以看出， 只有bd中AbstractBeanDefinition.factoryMethodName属性不为空才不可以作为配置类，其他bd 对应的class都需要作为配置类进行处理
    //所以基本每一个类都会被当作配置类递归处理，经过测试，可以看出@Configuration注解的类或者@Component注解的类，里面都可以带有@Import @ImportResource @Bean都可以，@Configuration里面可以有的注解，@Component都可以拥有
    //在扫描bd到bdmap阶段,@Configuration注解的类或者@Component注解的类作为配置类是完全一样的，我们只需要通过容器的构造函数参数传进去或者调用applicationContext.register(class)首先注册一个@Configuration注解的类或者@Component注解的类启动扫描过程就行(没有写错，手工注册的第一个bean被@Component或者@Configuration注解都可以，只要没有factorymethodname就好，启动扫描的过程是：1.从beanfactory拿出所有beanname, 调用checkConfigurationClassCandidate判断是不是一个配置类，是的话，启动递归处理)
    //@Component和@Configuration注解的类的区别在于，@Configuration注解的类，会生成一个代理类，来服务@bean method引入的bean的实例化， 这个代理类可以做到：@Bean中每次new bean之前，先使用beanFactory获取一下，获取到了不创建，没有获取到再创建，这样就不会违反spring的singleton bean规则。但是@Component注解的类就没有这个功能，所以最好不要在@Component注解的类里面写@Bean
    public void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {
		//定义一个list存放app 提供的bd（项目当中提供了@Compent）
		List<BeanDefinitionHolder> configCandidates = new ArrayList<>();
		//获取容器中注册的所有bd名字
		//7个: 6个spring内部的和1个我们的配置类的name
		String[] candidateNames = registry.getBeanDefinitionNames();

         /**
         * Full
		 * Lite
		 */
		for (String beanName : candidateNames) {
			BeanDefinition beanDef = registry.getBeanDefinition(beanName);
			if (ConfigurationClassUtils.isFullConfigurationClass(beanDef) ||
					ConfigurationClassUtils.isLiteConfigurationClass(beanDef)) {
				//果BeanDefinition中的configurationClass属性为full或者lite,则意味着已经处理过了,直接跳过
				//这里需要结合下面的代码才能理解
				if (logger.isDebugEnabled()) {
					logger.debug("Bean definition has already been processed as a configuration class: " + beanDef);
				}
			}
			//判断是否是Configuration类，如果加了Configuration下面的这几个注解就不再判断了
			// 还有  add(Component.class.getName());
			//		candidateIndicators.add(ComponentScan.class.getName());
			//		candidateIndicators.add(Import.class.getName());
			//		candidateIndicators.add(ImportResource.class.getName());
			//beanDef == appconfig
			else if (ConfigurationClassUtils.checkConfigurationClassCandidate(beanDef, this.metadataReaderFactory)) {
				//BeanDefinitionHolder 也可以看成一个数据结构
				configCandidates.add(new BeanDefinitionHolder(beanDef, beanName));
			}
		}

		// Return immediately if no @Configuration classes were found
		//通过configCandidates是否为空判断如果是不是一个@Configuration类， 如果不是，直接返回
		if (configCandidates.isEmpty()) {
			return;
		}


		// 排序，根据order,不重要, 因为可能有多个配置类，配置类如果加了@order注解的话，在这里进行排序
		//lamda表达式，后面的()->{}相当于创建一个匿名对象，这个匿名对象需要实现接口的方法，()里面是参数，{}里面是方法体
		// Sort by previously determined @Order value, if applicable
		configCandidates.sort((bd1, bd2) -> {
			int i1 = ConfigurationClassUtils.getOrder(bd1.getBeanDefinition());
			int i2 = ConfigurationClassUtils.getOrder(bd2.getBeanDefinition());
			return Integer.compare(i1, i2);
		});


		// Detect any custom bean name generation strategy supplied through the enclosing application context
		//检测应用程序上下文提供的自定义bean名称生成策略
		SingletonBeanRegistry sbr = null;
		// 如果BeanDefinitionRegistry是SingletonBeanRegistry子类的话,
		// 由于我们当前传入的是DefaultListableBeanFactory,是SingletonBeanRegistry 的子类// 因此会将registry强转为SingletonBeanRegistry
		if (registry instanceof SingletonBeanRegistry) {
			sbr = (SingletonBeanRegistry) registry;
			if (!this.localBeanNameGeneratorSet) {//是否有自定义的
				BeanNameGenerator generator = (BeanNameGenerator) sbr.getSingleton(CONFIGURATION_BEAN_NAME_GENERATOR);
				//SingletonBeanRegistry中有id为 org.springframework.context.annotation.internalConfigurationBeanNameGenerator
				//如果有则使用它，否则使用spring默认的
				if (generator != null) {
					this.componentScanBeanNameGenerator = generator;
					this.importBeanNameGenerator = generator;
				}
			}
		}

		if (this.environment == null) {
			this.environment = new StandardEnvironment();
		}

		// Parse each @Configuration class
		//实例化ConfigurationClassParser 为了解析各个配置类
		ConfigurationClassParser parser = new ConfigurationClassParser(
				this.metadataReaderFactory, this.problemReporter, this.environment,
				this.resourceLoader, this.componentScanBeanNameGenerator, registry);

		//candidates用于将之前加入的configCandidates进行去重
		//因为可能有多个配置类重复了，spring不可能重复，因为我们可以自定义的加进来可能会导致重复
		//list->set的方式去除重复
		Set<BeanDefinitionHolder> candidates = new LinkedHashSet<>(configCandidates);
		//alreadyParsed存放处理过的
		Set<ConfigurationClass> alreadyParsed = new HashSet<>(configCandidates.size());
		do {
		    //并且把扫描出的类的bd放进bdmap中, 并且会解析@bean method/@ImportResource/ @Import /ImportBeanDefinitionRegistrar 填充configClasses,然后this.reader.loadBeanDefinitions(configClasses)再注册这些bd到bdmap
            //什么是配置类 ： 具体看Full Lite配置类的判断
			parser.parse(candidates);
			parser.validate();
			//map.keyset
			Set<ConfigurationClass> configClasses = new LinkedHashSet<>(parser.getConfigurationClasses());
			configClasses.removeAll(alreadyParsed);

			// Read the model and create bean definitions based on its content
			if (this.reader == null) {
				this.reader = new ConfigurationClassBeanDefinitionReader(
						registry, this.sourceExtractor, this.resourceLoader, this.environment,
						this.importBeanNameGenerator, parser.getImportRegistry());
			}

			//上面的代码运行完ConfigurationClass的importedBy beanMethods importedResources importBeanDefinitionRegistrars信息已经被填充
            //this.reader.loadBeanDefinitions(configClasses)方法里面处理@bean method引入的bean  @ImportResource引入的xml配置的bean ImportBeanDefinitionRegistrar引入的bean的bd的注册，  @import引入的配置类 的处理
            //spring boot上面的扫描完成后，到这里有好几十个配置类(@Configuration注解的类)
			this.reader.loadBeanDefinitions(configClasses);

			alreadyParsed.addAll(configClasses);

			candidates.clear();

			//由于我们这里进行了扫描，把扫描出来的BeanDefinition注册给了factory
			//但是新注入bdmap的bean需要当成配置类再处理一次, 直到所有注册到bdmap的bean都被当成配置类处理一次，整个do while循环才结束
			if (registry.getBeanDefinitionCount() > candidateNames.length) {
				String[] newCandidateNames = registry.getBeanDefinitionNames();
				Set<String> oldCandidateNames = new HashSet<>(Arrays.asList(candidateNames));
				Set<String> alreadyParsedClasses = new HashSet<>();
				for (ConfigurationClass configurationClass : alreadyParsed) {
					alreadyParsedClasses.add(configurationClass.getMetadata().getClassName());
				}
				for (String candidateName : newCandidateNames) {
					if (!oldCandidateNames.contains(candidateName)) {
						BeanDefinition bd = registry.getBeanDefinition(candidateName);
						if (ConfigurationClassUtils.checkConfigurationClassCandidate(bd, this.metadataReaderFactory) &&
								!alreadyParsedClasses.contains(bd.getBeanClassName())) {
							candidates.add(new BeanDefinitionHolder(bd, candidateName));
						}
					}
				}
				candidateNames = newCandidateNames;
			}
		}
		while (!candidates.isEmpty());

		// Register the ImportRegistry as a bean in order to support ImportAware @Configuration classes:
        // 将ImportRegistry注册为一个bean，以便支持支持ImportAware @Configuration类
        // 这里直接往singletonObjects添加一个ConfigurationClassParser$ImportStack的bean(bdmap没有与之对应的值),如下
        // name : org.springframework.context.annotation.ConfigurationClassPostProcessor.importRegistry  bean : ConfigurationClassParser$ImportStack
        // 全局搜索IMPORT_REGISTRY_BEAN_NAME  发现在#ConfiguratinoClassPostProcessor#postProcessBeforeInitialization中处理
		if (sbr != null && !sbr.containsSingleton(IMPORT_REGISTRY_BEAN_NAME)) {
			sbr.registerSingleton(IMPORT_REGISTRY_BEAN_NAME, parser.getImportRegistry());
		}

		if (this.metadataReaderFactory instanceof CachingMetadataReaderFactory) {
			// Clear cache in externally provided MetadataReaderFactory; this is a no-op
			// for a shared cache since it'll be cleared by the ApplicationContext.
			((CachingMetadataReaderFactory) this.metadataReaderFactory).clearCache();
		}
	}

	/**
	 * Post-processes a BeanFactory in search of Configuration class BeanDefinitions;
	 * any candidates are then enhanced by a {@link ConfigurationClassEnhancer}.
	 * Candidate status is determined by BeanDefinition attribute metadata.
	 * @see ConfigurationClassEnhancer
	 */
    //ConfigurationClassPostProcessor.postProcessBeanDefinitionRegistry() ==> processConfigBeanDefinitions(registry) 完成bd到bdmap的过程
    //(并且在bd中标记了是全配置类还是半配置类，这个信息只是在enhanceConfigurationClasses()中使用，扫描注册bd时值看是不是配置类，不关心是全配置类还是半配置类)
    //ConfigurationClassPostProcessor.postProcessBeanFactory() ==>enhanceConfigurationClasses()给全配置类生成代理
    //从上面的调用顺序可以看到，扫描注册bd到bdmap全部完成后，才会给全配置类生成代理，所以给配置类生成代理跟扫描bd到bdmap之间没有关系
    //给全配置类生成代理，只是为了给@bean method引入的bean的实例化过程服务的
	public void enhanceConfigurationClasses(ConfigurableListableBeanFactory beanFactory) {
		Map<String, AbstractBeanDefinition> configBeanDefs = new LinkedHashMap<>();
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);

			//判断是否是一个全注解类
			if (ConfigurationClassUtils.isFullConfigurationClass(beanDef)) {

			    //不能增强@Configuration bean definiton,因为这个类不是AbstractBeanDefinition的子类
				if (!(beanDef instanceof AbstractBeanDefinition)) {
					throw new BeanDefinitionStoreException("Cannot enhance @Configuration bean definition '" +
							beanName + "' since it is not stored in an AbstractBeanDefinition subclass");
				}

                // 不能增强@Configuration bean definiton,因为这个类里面定义了一个非static的@bean method, 并且这个@bean method返回BeanDefinitionRegistryPostProcessor类型，
                // 考虑将这个@bean method改为static方法。
                // 为什么要有这个警告呢？我们可以思考下，静态方法加载类的时候就会执行，目的就是尽可能提前引入BeanDefinitionRegistryPostProcessor到spring容器，太晚的话spring可能没有机会执行BeanDefinitionRegistryPostProcessor.postProcessBeanDefinitionRegistry
                // 下面是BeanDefinitionRegistryPostProcessor.postProcessBeanDefinitionRegistry的触发点
                // refresh() ==> invokeBeanFactoryPostProcessors() ==> 调用了2次invokeBeanDefinitionRegistryPostProcessors(),Spring Framework本身只有一个BeanDefinitionRegistryPostProcessors实现类ConfigurationClassPostProcessor
				else if (logger.isWarnEnabled() && beanFactory.containsSingleton(beanName)) {
					logger.warn("Cannot enhance @Configuration bean definition '" + beanName +
							"' since its singleton instance has been created too early. The typical cause " +
							"is a non-static @Bean method with a BeanDefinitionRegistryPostProcessor " +
							"return type: Consider declaring such methods as 'static'.");
				}
				configBeanDefs.put(beanName, (AbstractBeanDefinition) beanDef);
			}
		}
		if (configBeanDefs.isEmpty()) {
			// nothing to enhance -> return immediately
			return;
		}

		ConfigurationClassEnhancer enhancer = new ConfigurationClassEnhancer();
		for (Map.Entry<String, AbstractBeanDefinition> entry : configBeanDefs.entrySet()) {
			AbstractBeanDefinition beanDef = entry.getValue();
			// If a @Configuration class gets proxied, always proxy the target class
			beanDef.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
			try {
				// Set enhanced subclass of the user-specified bean class
				Class<?> configClass = beanDef.resolveBeanClass(this.beanClassLoader);
				if (configClass != null) {
					//完成对全注解类的cglib代理
					Class<?> enhancedClass = enhancer.enhance(configClass, this.beanClassLoader);
					if (configClass != enhancedClass) {
						if (logger.isDebugEnabled()) {
							logger.debug(String.format("Replacing bean definition '%s' existing class '%s' with " +
									"enhanced class '%s'", entry.getKey(), configClass.getName(), enhancedClass.getName()));
						}
						beanDef.setBeanClass(enhancedClass);
					}
				}
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Cannot load configuration class: " + beanDef.getBeanClassName(), ex);
			}
		}
	}


	private static class ImportAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter {

		private final BeanFactory beanFactory;



		public ImportAwareBeanPostProcessor(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		@Override
		public PropertyValues postProcessPropertyValues(
				PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) {

			// Inject the BeanFactory before AutowiredAnnotationBeanPostProcessor's
			// postProcessPropertyValues method attempts to autowire other configuration beans.
			if (bean instanceof EnhancedConfiguration) {
				((EnhancedConfiguration) bean).setBeanFactory(this.beanFactory);
			}
			return pvs;
		}

		@Override
        //这个方法处理实现ImportAware接口的bean
		public Object postProcessBeforeInitialization(Object bean, String beanName)  {
			if (bean instanceof ImportAware) {
                //ConfigurationClassPostProcessor.postProcessBeanFactory#processConfigBeanDefinitions#sbr.registerSingleton(IMPORT_REGISTRY_BEAN_NAME, parser.getImportRegistry())
                //上面的代码会注册一个org.springframework.context.annotation.ConfigurationClassPostProcessor.importRegistry 的bean， 直接放在singletonObjects中，不会进bdmap
                // 这个bean管理了 配置类的注解元数据 和 配置类上@import注解引入的类 的对应关系
                ImportRegistry ir = this.beanFactory.getBean(IMPORT_REGISTRY_BEAN_NAME, ImportRegistry.class);
				AnnotationMetadata importingClass = ir.getImportingClassFor(bean.getClass().getSuperclass().getName());
				if (importingClass != null) {
					((ImportAware) bean).setImportMetadata(importingClass);
				}
			}
			return bean;
		}
	}

}

/*
 * Copyright 2002-2017 the original author or authors.
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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.index.CandidateComponentsIndex;
import org.springframework.context.index.CandidateComponentsIndexLoader;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A component provider that provides candidate components from a base package. Can
 * use {@link CandidateComponentsIndex the index} if it is available of scans the
 * classpath otherwise. Candidate components are identified by applying exclude and
 * include filters. {@link AnnotationTypeFilter}, {@link AssignableTypeFilter} include
 * filters on an annotation/superclass that are annotated with {@link Indexed} are
 * supported: if any other include filter is specified, the index is ignored and
 * classpath scanning is used instead.
 *
 * <p>This implementation is based on Spring's
 * {@link org.springframework.core.type.classreading.MetadataReader MetadataReader}
 * facility, backed by an ASM {@link org.springframework.asm.ClassReader ClassReader}.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 2.5
 * @see org.springframework.core.type.classreading.MetadataReaderFactory
 * @see org.springframework.core.type.AnnotationMetadata
 * @see ScannedGenericBeanDefinition
 * @see CandidateComponentsIndex
 */
// 下面用一个例子说明怎么使用。例子实现了动态注册继承BaseMapper接口的bean，我们可以通过是否引入ImportBeanDefinitionRegistrar就可以决定是否注册继承BaseMapper接口的bean
// 是否引入ImportBeanDefinitionRegistrar我们可以用一个enable注解来做，这样就很容易维护
//
// 1. 声明一个BaseMapper接口和一个实现类
//
// public interface BaseMapper {
// }
// public class PersonMapper implements BaseMapper{
// }
// 2. 声明一个MapperScan的注解，里面声明扫描路径，并且通过@Import注解引入ImportBeanDefinitionRegistrar的实现类。ImportBeanDefinitionRegistrar 是 动态注册bd的扩展类，在容器的bd注册阶段调用。
//
// @Retention(RetentionPolicy.RUNTIME)
// @Target(ElementType.TYPE)
// @Documented
// @Import(MapperRegistrar.class)
// public @interface MapperScan {
//     /**
//      * 扫描的基础包名
//      */
//     String[] basePackages() default {};
//
//     /**
//      * 扫描包名的提供类
//      */
//     Class<?>[] basePackageClasses() default {};
// }
// 3. 实现 ImportBeanDefinitionRegistrar ，并通过 ClassPathScanningCandidateComponentProvider 获取bd。
//
// public class MapperRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {
//     private Environment environment;
//
//     @Override
//     public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
//
//         //聚合扫描的路径
//         Set<String> basePackages = getBasePackages(importingClassMetadata);
//
//         //获取扫描器
//         ClassPathScanningCandidateComponentProvider scanner = getScanner();
//
//         //扫描并注册bd
//         for (String basePackage : basePackages) {
//             Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);
//             for (BeanDefinition beanDefinition : candidateComponents) {
//                 //注册BD
//                 String beanClassName = beanDefinition.getBeanClassName();
//                 registry.registerBeanDefinition(beanClassName, beanDefinition);
//             }
//         }
//
//     }
//
//     private ClassPathScanningCandidateComponentProvider getScanner() {
//         //不使用默认的过滤器
//         ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false, environment);
//         scanner.addIncludeFilter(new TypeFilter() {
//             @Override
//             public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
//                 //继承BaseMapper接口，并且是具体类
//                 AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
//                 String[] interfaceNames = metadata.getInterfaceNames();
//
//                 return isImplement(interfaceNames, BaseMapper.class) && metadata.isConcrete()
//                         && !metadata.isInterface() && !metadata.isAbstract() && !metadata.isAnnotation();
//             }
//
//             private boolean isImplement(String[] interfaceNames, Class<BaseMapper> targetInterface) {
//                 if (interfaceNames != null && targetInterface != null) {
//                     return Arrays.asList(interfaceNames).contains(targetInterface.getCanonicalName());
//                 }
//                 return false;
//             }
//         });
//
//         return scanner;
//     }
//
//     /**
//      * 获取扫描组件的包路径
//      */
//     protected Set<String> getBasePackages(AnnotationMetadata importingClassMetadata) {
//         Map<String, Object> attributes = importingClassMetadata
//                 .getAnnotationAttributes(MapperScan.class.getCanonicalName());
//
//         Set<String> basePackages = new HashSet<>();
//
//         for (String pkg : (String[]) attributes.get("basePackages")) {
//             if (StringUtils.hasText(pkg)) {
//                 basePackages.add(pkg);
//             }
//         }
//         for (Class<?> clazz : (Class<?>[]) attributes.get("basePackageClasses")) {
//             basePackages.add(ClassUtils.getPackageName(clazz));
//         }
//
//         if (basePackages.isEmpty()) {
//             basePackages.add(
//                     ClassUtils.getPackageName(importingClassMetadata.getClassName()));
//         }
//         return basePackages;
//     }
//
//     @Override
//     public void setEnvironment(Environment environment) {
//         this.environment = environment;
//     }
// }

//另外一个例子：
//Spring boot的@ServletComponentScan==>@Import(ServletComponentScanRegistrar.class)
//这个ServletComponentScanRegistrar里面会注册servletComponentRegisteringPostProcessor，
//servletComponentRegisteringPostProcessor 继承BeanFactoryPostProcessor
//ServletComponentRegisteringPostProcessor#postProcessBeanFactory也是直接使用ClassPathScanningCandidateComponentProvider扫描和注册Filter Servlet Listener

//ClassPathScanningCandidateComponentProvider是Spring提供的工具，可以按自定义的类型，查找classpath下符合要求的class文件。
//spring使用的扫描器ClassPathBeanDefinitionScanner就继承自这个类
public class ClassPathScanningCandidateComponentProvider implements EnvironmentCapable, ResourceLoaderAware {

    //后面的代码会用这个拼接扫描路径 ：
    //Spring拼接的扫描地址：classpath*:xxx/xxx/xxx/**/*.class
    //String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + resolveBasePackage(basePackage) + '/' + this.resourcePattern;
	static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";


	protected final Log logger = LogFactory.getLog(getClass());

	private String resourcePattern = DEFAULT_RESOURCE_PATTERN;

	//过滤器列表。过滤后的类被认为是候选组件
	private final List<TypeFilter> includeFilters = new LinkedList<>();

	//excludeFilters：过滤器列表。排除在候选组件之外
	private final List<TypeFilter> excludeFilters = new LinkedList<>();

	@Nullable
	private Environment environment;

	@Nullable
	private ConditionEvaluator conditionEvaluator;

	@Nullable
	private ResourcePatternResolver resourcePatternResolver;

	@Nullable
	private MetadataReaderFactory metadataReaderFactory;

	@Nullable
	private CandidateComponentsIndex componentsIndex;


	/**
	 * Protected constructor for flexible subclass initialization.
	 * @since 4.3.6
	 */
	protected ClassPathScanningCandidateComponentProvider() {
	}

	/**
	 * Create a ClassPathScanningCandidateComponentProvider with a {@link StandardEnvironment}.
	 * @param useDefaultFilters whether to register the default filters for the
	 * {@link Component @Component}, {@link Repository @Repository},
	 * {@link Service @Service}, and {@link Controller @Controller}
	 * stereotype annotations
	 * @see #registerDefaultFilters()
	 */
    // true：默认TypeFilter生效，这种模式会查询出许多不符合你要求的class名
    // false：关闭默认TypeFilter
	public ClassPathScanningCandidateComponentProvider(boolean useDefaultFilters) {
		this(useDefaultFilters, new StandardEnvironment());
	}

	/**
	 * Create a ClassPathScanningCandidateComponentProvider with the given {@link Environment}.
	 * @param useDefaultFilters whether to register the default filters for the
	 * {@link Component @Component}, {@link Repository @Repository},
	 * {@link Service @Service}, and {@link Controller @Controller}
	 * stereotype annotations
	 * @param environment the Environment to use
	 * @see #registerDefaultFilters()
	 */
	public ClassPathScanningCandidateComponentProvider(boolean useDefaultFilters, Environment environment) {
		if (useDefaultFilters) {
			registerDefaultFilters();
		}
		setEnvironment(environment);
		setResourceLoader(null);
	}


	/**
	 * Set the resource pattern to use when scanning the classpath.
	 * This value will be appended to each base package name.
	 * @see #findCandidateComponents(String)
	 * @see #DEFAULT_RESOURCE_PATTERN
	 */
	public void setResourcePattern(String resourcePattern) {
		Assert.notNull(resourcePattern, "'resourcePattern' must not be null");
		this.resourcePattern = resourcePattern;
	}

	/**
	 * Add an include type filter to the <i>end</i> of the inclusion list.
	 */
	// 使用示例 ：
    // 扫描带有自定义注解的类
    // provider.addIncludeFilter(new AnnotationTypeFilter(ScanAnnotation.class));
    // 接口不会被扫描，其子类会被扫描出来
    // provider.addIncludeFilter(new AssignableTypeFilter(ScanClassInterface.class));
	public void addIncludeFilter(TypeFilter includeFilter) {
		this.includeFilters.add(includeFilter);
	}

	/**
	 * Add an exclude type filter to the <i>front</i> of the exclusion list.
	 */
	public void addExcludeFilter(TypeFilter excludeFilter) {
		this.excludeFilters.add(0, excludeFilter);
	}

	/**
	 * Reset the configured type filters.
	 * @param useDefaultFilters whether to re-register the default filters for
	 * the {@link Component @Component}, {@link Repository @Repository},
	 * {@link Service @Service}, and {@link Controller @Controller}
	 * stereotype annotations
	 * @see #registerDefaultFilters()
	 */
	//重置filter
	public void resetFilters(boolean useDefaultFilters) {
		this.includeFilters.clear();
		this.excludeFilters.clear();
		if (useDefaultFilters) {
			registerDefaultFilters();
		}
	}

	/**
	 * Register the default filter for {@link Component @Component}.
	 * <p>This will implicitly register all annotations that have the
	 * {@link Component @Component} meta-annotation including the
	 * {@link Repository @Repository}, {@link Service @Service}, and
	 * {@link Controller @Controller} stereotype annotations.
	 * <p>Also supports Java EE 6's {@link javax.annotation.ManagedBean} and
	 * JSR-330's {@link javax.inject.Named} annotations, if available.
	 *
	 */
	@SuppressWarnings("unchecked")
	protected void registerDefaultFilters() {
		this.includeFilters.add(new AnnotationTypeFilter(Component.class));
		ClassLoader cl = ClassPathScanningCandidateComponentProvider.class.getClassLoader();
		try {
			this.includeFilters.add(new AnnotationTypeFilter(
					((Class<? extends Annotation>) ClassUtils.forName("javax.annotation.ManagedBean", cl)), false));
			logger.debug("JSR-250 'javax.annotation.ManagedBean' found and supported for component scanning");
		}
		catch (ClassNotFoundException ex) {
			// JSR-250 1.1 API (as included in Java EE 6) not available - simply skip.
		}
		try {
			this.includeFilters.add(new AnnotationTypeFilter(
					((Class<? extends Annotation>) ClassUtils.forName("javax.inject.Named", cl)), false));
			logger.debug("JSR-330 'javax.inject.Named' annotation found and supported for component scanning");
		}
		catch (ClassNotFoundException ex) {
			// JSR-330 API not available - simply skip.
		}
	}

	/**
	 * Set the Environment to use when resolving placeholders and evaluating
	 * {@link Conditional @Conditional}-annotated component classes.
	 * <p>The default is a {@link StandardEnvironment}.
	 * @param environment the Environment to use
	 */
	public void setEnvironment(Environment environment) {
		Assert.notNull(environment, "Environment must not be null");
		this.environment = environment;
		this.conditionEvaluator = null;
	}

	@Override
	public final Environment getEnvironment() {
		if (this.environment == null) {
			this.environment = new StandardEnvironment();
		}
		return this.environment;
	}

	/**
	 * Return the {@link BeanDefinitionRegistry} used by this scanner, if any.
	 */
	@Nullable
	protected BeanDefinitionRegistry getRegistry() {
		return null;
	}

	/**
	 * Set the {@link ResourceLoader} to use for resource locations.
	 * This will typically be a {@link ResourcePatternResolver} implementation.
	 * <p>Default is a {@code PathMatchingResourcePatternResolver}, also capable of
	 * resource pattern resolving through the {@code ResourcePatternResolver} interface.
	 * @see org.springframework.core.io.support.ResourcePatternResolver
	 * @see org.springframework.core.io.support.PathMatchingResourcePatternResolver
	 */
	@Override
	public void setResourceLoader(@Nullable ResourceLoader resourceLoader) {
		this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
		this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
		this.componentsIndex = CandidateComponentsIndexLoader.loadIndex(this.resourcePatternResolver.getClassLoader());
	}

	/**
	 * Return the ResourceLoader that this component provider uses.
	 */
	public final ResourceLoader getResourceLoader() {
		return getResourcePatternResolver();
	}

	private ResourcePatternResolver getResourcePatternResolver() {
		if (this.resourcePatternResolver == null) {
			this.resourcePatternResolver = new PathMatchingResourcePatternResolver();
		}
		return this.resourcePatternResolver;
	}

	/**
	 * Set the {@link MetadataReaderFactory} to use.
	 * <p>Default is a {@link CachingMetadataReaderFactory} for the specified
	 * {@linkplain #setResourceLoader resource loader}.
	 * <p>Call this setter method <i>after</i> {@link #setResourceLoader} in order
	 * for the given MetadataReaderFactory to override the default factory.
	 */
	public void setMetadataReaderFactory(MetadataReaderFactory metadataReaderFactory) {
		this.metadataReaderFactory = metadataReaderFactory;
	}

	/**
	 * Return the MetadataReaderFactory used by this component provider.
	 */
	public final MetadataReaderFactory getMetadataReaderFactory() {
		if (this.metadataReaderFactory == null) {
			this.metadataReaderFactory = new CachingMetadataReaderFactory();
		}
		return this.metadataReaderFactory;
	}


	/**
	 * Scan the class path for candidate components.
	 * @param basePackage the package to check for annotated classes
	 * @return a corresponding Set of autodetected bean definitions
	 * 利用asm完成扫描，asm框架是一个致力于字节码操作和分析的框架
	 * 因为我们的class文件被加载到jvm中，已经是字节码形式的了，所以我们需要这样一个框架来解析
	 */
	//扫描指定的包路径，获取相应的BeanDefinition。扫描后的类可以通过过滤器进行排除。
	public Set<BeanDefinition> findCandidateComponents(String basePackage) {
		if (this.componentsIndex != null && indexSupportsIncludeFilters()) {
			//前面讲到过，我们可以指定扫描路径使用索引，这就会使用索引来扫描类
			return addCandidateComponentsFromIndex(this.componentsIndex, basePackage);
		}
		else {
			return scanCandidateComponents(basePackage);
		}
	}

	/**
	 * Determine if the index can be used by this instance.
	 * @return {@code true} if the index is available and the configuration of this
	 * instance is supported by it, {@code false} otherwise
	 * @since 5.0
	 */
	private boolean indexSupportsIncludeFilters() {
		for (TypeFilter includeFilter : this.includeFilters) {
			if (!indexSupportsIncludeFilter(includeFilter)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Determine if the specified include {@link TypeFilter} is supported by the index.
	 * @param filter the filter to check
	 * @return whether the index supports this include filter
	 * @since 5.0
	 * @see #extractStereotype(TypeFilter)
	 */
	private boolean indexSupportsIncludeFilter(TypeFilter filter) {
		if (filter instanceof AnnotationTypeFilter) {
			Class<? extends Annotation> annotation = ((AnnotationTypeFilter) filter).getAnnotationType();
			return (AnnotationUtils.isAnnotationDeclaredLocally(Indexed.class, annotation) ||
					annotation.getName().startsWith("javax."));
		}
		if (filter instanceof AssignableTypeFilter) {
			Class<?> target = ((AssignableTypeFilter) filter).getTargetType();
			return AnnotationUtils.isAnnotationDeclaredLocally(Indexed.class, target);
		}
		return false;
	}

	/**
	 * Extract the stereotype to use for the specified compatible filter.
	 * @param filter the filter to handle
	 * @return the stereotype in the index matching this filter
	 * @since 5.0
	 * @see #indexSupportsIncludeFilter(TypeFilter)
	 */
	@Nullable
	private String extractStereotype(TypeFilter filter) {
		if (filter instanceof AnnotationTypeFilter) {
			return ((AnnotationTypeFilter) filter).getAnnotationType().getName();
		}
		if (filter instanceof AssignableTypeFilter) {
			return ((AssignableTypeFilter) filter).getTargetType().getName();
		}
		return null;
	}

	//前面讲到过，我们可以指定扫描路径使用索引，这就会使用索引来扫描类
	private Set<BeanDefinition> addCandidateComponentsFromIndex(CandidateComponentsIndex index, String basePackage) {
		Set<BeanDefinition> candidates = new LinkedHashSet<>();
		try {
			Set<String> types = new HashSet<>();
			for (TypeFilter filter : this.includeFilters) {
				String stereotype = extractStereotype(filter);
				if (stereotype == null) {
					throw new IllegalArgumentException("Failed to extract stereotype from "+ filter);
				}
				types.addAll(index.getCandidateTypes(basePackage, stereotype));
			}
			boolean traceEnabled = logger.isTraceEnabled();
			boolean debugEnabled = logger.isDebugEnabled();
			for (String type : types) {
				MetadataReader metadataReader = getMetadataReaderFactory().getMetadataReader(type);
				if (isCandidateComponent(metadataReader)) {
					AnnotatedGenericBeanDefinition sbd = new AnnotatedGenericBeanDefinition(
							metadataReader.getAnnotationMetadata());
					if (isCandidateComponent(sbd)) {
						if (debugEnabled) {
							logger.debug("Using candidate component class from index: " + type);
						}
						candidates.add(sbd);
					}
					else {
						if (debugEnabled) {
							logger.debug("Ignored because not a concrete top-level class: " + type);
						}
					}
				}
				else {
					if (traceEnabled) {
						logger.trace("Ignored because matching an exclude filter: " + type);
					}
				}
			}
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException("I/O failure during classpath scanning", ex);
		}
		return candidates;
	}

	private Set<BeanDefinition> scanCandidateComponents(String basePackage) {
		Set<BeanDefinition> candidates = new LinkedHashSet<>();
		try {
		    //Spring拼接的扫描地址：classpath*:xxx/xxx/xxx/**/*.class
			String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
					resolveBasePackage(basePackage) + '/' + this.resourcePattern;
			//使用ResourceLoader读取class文件，将InputStream封装为Resource[]返回, 使用Resource构造beandifinition对象
			Resource[] resources = getResourcePatternResolver().getResources(packageSearchPath);
			boolean traceEnabled = logger.isTraceEnabled();
			boolean debugEnabled = logger.isDebugEnabled();
			for (Resource resource : resources) {
				if (traceEnabled) {
					logger.trace("Scanning " + resource);
				}
				if (resource.isReadable()) {
					try {
					    //使用resource(class文件)创建一个MetadataReader，MetadataReader相当于一个门面，用来访问class的元数据
						MetadataReader metadataReader = getMetadataReaderFactory().getMetadataReader(resource);
						//isCandidateComponent(metadataReader)点进去有注释
						if (isCandidateComponent(metadataReader)) {

						    //可以看出，扫描方式创建的bd类型是ScannedGenericBeanDefinition
                            //把metadataReader传进构造方法
							ScannedGenericBeanDefinition sbd = new ScannedGenericBeanDefinition(metadataReader);
							sbd.setResource(resource);
							sbd.setSource(resource);
							//isCandidateComponent(sbd))点进去有注释
							if (isCandidateComponent(sbd)) {
								if (debugEnabled) {
									logger.debug("Identified candidate component class: " + resource);
								}
								candidates.add(sbd);
							}
							else {
								if (debugEnabled) {
									logger.debug("Ignored because not a concrete top-level class: " + resource);
								}
							}
						}
						else {
							if (traceEnabled) {
								logger.trace("Ignored because not matching any filter: " + resource);
							}
						}
					}
					catch (Throwable ex) {
						throw new BeanDefinitionStoreException(
								"Failed to read candidate component class: " + resource, ex);
					}
				}
				else {
					if (traceEnabled) {
						logger.trace("Ignored because not readable: " + resource);
					}
				}
			}
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException("I/O failure during classpath scanning", ex);
		}
		return candidates;
	}


	/**
	 * Resolve the specified base package into a pattern specification for
	 * the package search path.
	 * <p>The default implementation resolves placeholders against system properties,
	 * and converts a "."-based package path to a "/"-based resource path.
	 * @param basePackage the base package as specified by the user
	 * @return the pattern specification to be used for package searching
	 */
	protected String resolveBasePackage(String basePackage) {
		return ClassUtils.convertClassNameToResourcePath(getEnvironment().resolveRequiredPlaceholders(basePackage));
	}

	/**
	 * Determine whether the given class does not match any exclude filter
	 * and does match at least one include filter.
	 * @param metadataReader the ASM ClassReader for the class
	 * @return whether the class qualifies as a candidate component
	 */
	//根据指定的扫描规则，比如排除，包含等规则来判断是否排除和包含
	//我们配置的bean扫描路径里面的排除 包含规则就在这里判断生效
	protected boolean isCandidateComponent(MetadataReader metadataReader) throws IOException {
		for (TypeFilter tf : this.excludeFilters) {
			if (tf.match(metadataReader, getMetadataReaderFactory())) {
				return false;
			}
		}
		for (TypeFilter tf : this.includeFilters) {
			if (tf.match(metadataReader, getMetadataReaderFactory())) {
			    //根据bean的@Condition注解信息判断是否满足条件，依次决定是否应该扫描
				return isConditionMatch(metadataReader);
			}
		}
		return false;
	}

	/**
	 * Determine whether the given class is a candidate component based on any
	 * {@code @Conditional} annotations.
	 * @param metadataReader the ASM ClassReader for the class
	 * @return whether the class qualifies as a candidate component
	 */
	private boolean isConditionMatch(MetadataReader metadataReader) {
		if (this.conditionEvaluator == null) {
			this.conditionEvaluator =
					new ConditionEvaluator(getRegistry(), this.environment, this.resourcePatternResolver);
		}
		return !this.conditionEvaluator.shouldSkip(metadataReader.getAnnotationMetadata());
	}

	/**
	 * Determine whether the given bean definition qualifies as candidate.
	 * <p>The default implementation checks whether the class is not an interface
	 * and not dependent on an enclosing class.
	 * <p>Can be overridden in subclasses.
	 * @param beanDefinition the bean definition to check
	 * @return whether the bean definition qualifies as a candidate component
	 */
	//判断bd是否是一个接口，有没有Lookup注解，是不是抽象类等，以此判断能不能被创建
    //判断通过filter筛选后的class是否是候选组件，默认实现是一个具体类。这是一个 protected 的方法，可以通过子类重写它。
    //有些框架只需要扫描接口，并注册FactoryBean到bd，然后通过动态代理实现该接口得到目标bean，比如feign
	protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
		AnnotationMetadata metadata = beanDefinition.getMetadata();
		return (metadata.isIndependent() && (metadata.isConcrete() ||
				(metadata.isAbstract() && metadata.hasAnnotatedMethods(Lookup.class.getName()))));
	}


	/**
	 * Clear the local metadata cache, if any, removing all cached class metadata.
	 */
	public void clearCache() {
		if (this.metadataReaderFactory instanceof CachingMetadataReaderFactory) {
			// Clear cache in externally provided MetadataReaderFactory; this is a no-op
			// for a shared cache since it'll be cleared by the ApplicationContext.
			((CachingMetadataReaderFactory) this.metadataReaderFactory).clearCache();
		}
	}

}

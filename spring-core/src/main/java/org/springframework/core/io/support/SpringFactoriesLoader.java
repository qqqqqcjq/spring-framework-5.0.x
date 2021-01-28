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

package org.springframework.core.io.support;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.UrlResource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * General purpose factory loading mechanism for internal use within the framework.
 *
 * <p>{@code SpringFactoriesLoader} {@linkplain #loadFactories loads} and instantiates
 * factories of a given type from {@value #FACTORIES_RESOURCE_LOCATION} files which
 * may be present in multiple JAR files in the classpath. The {@code spring.factories}
 * file must be in {@link Properties} format, where the key is the fully qualified
 * name of the interface or abstract class, and the value is a comma-separated list of
 * implementation class names. For example:
 *
 * <pre class="code">example.MyService=example.MyServiceImpl1,example.MyServiceImpl2</pre>
 *
 * where {@code example.MyService} is the name of the interface, and {@code MyServiceImpl1}
 * and {@code MyServiceImpl2} are two implementations.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.2
 */
// spring的一个工具类， 用于使用类加载器加载类到JVM
// spring boot 的 SPI 可插拔实现方式就是 ： 使用这种方式加载 META-INF/spring.factories下的配置的类到应用中
// 和java 本身自带的一个类的功能很相似 ：java.util.ServiceLoader
// 只需要在classpath路径下新建一个文件META-INF/spring.factories，并在里面按照Properties格式填写好借口和实现类即可通过SpringFactoriesLoader来实例化相应的Bean。其中key可以是接口、注解、或者抽象类的全名。value为相应的实现类，当存在多个实现类时，用“,”进行分割
public abstract class SpringFactoriesLoader {

	/**
	 * The location to look for factories.
	 * <p>Can be present in multiple JAR files.
	 */
	//META-INF/spring.factories 指明在那个文件中查找
	//可以出现在多个JAR文件中
	public static final String FACTORIES_RESOURCE_LOCATION = "META-INF/spring.factories";


	private static final Log logger = LogFactory.getLog(SpringFactoriesLoader.class);

    //用来缓存MultiValueMap对象
	private static final Map<ClassLoader, MultiValueMap<String, String>> cache = new ConcurrentReferenceHashMap<>();


	/**
	 * Load and instantiate the factory implementations of the given type from
	 * {@value #FACTORIES_RESOURCE_LOCATION}, using the given class loader.
	 * <p>The returned factories are sorted through {@link AnnotationAwareOrderComparator}.
	 * <p>If a custom instantiation strategy is required, use {@link #loadFactoryNames}
	 * to obtain all registered factory names.
	 * @param factoryClass the interface or abstract class representing the factory
	 * @param classLoader the ClassLoader to use for loading (can be {@code null} to use the default)
	 * @see #loadFactoryNames
	 * @throws IllegalArgumentException if any factory implementation class cannot
	 * be loaded or if an error occurs while instantiating any factory
	 */
	//  根据给定的类型加载并实例化实现类
	public static <T> List<T> loadFactories(Class<T> factoryClass, @Nullable ClassLoader classLoader) {
		Assert.notNull(factoryClass, "'factoryClass' must not be null");
		// 获取类加载器
		ClassLoader classLoaderToUse = classLoader;
		if (classLoaderToUse == null) {
			classLoaderToUse = SpringFactoriesLoader.class.getClassLoader();
		}
        //加载类的全限定名
		List<String> factoryNames = loadFactoryNames(factoryClass, classLoaderToUse);
		if (logger.isTraceEnabled()) {
			logger.trace("Loaded [" + factoryClass.getName() + "] names: " + factoryNames);
		}
        //创建一个存放对象的List
		List<T> result = new ArrayList<>(factoryNames.size());
		for (String factoryName : factoryNames) {
            //实例化Bean,并将Bean放入到List集合中
			result.add(instantiateFactory(factoryName, factoryClass, classLoaderToUse));
		}
        //对List中的Bean进行排序
		AnnotationAwareOrderComparator.sort(result);
		return result;
	}

	/**
	 * Load the fully qualified class names of factory implementations of the
	 * given type from {@value #FACTORIES_RESOURCE_LOCATION}, using the given
	 * class loader.
	 * @param factoryClass the interface or abstract class representing the factory
	 * @param classLoader the ClassLoader to use for loading resources; can be
	 * {@code null} to use the default
	 * @see #loadFactories
	 * @throws IllegalArgumentException if an error occurs while loading factory names
	 */
	//根据给定的类型加载类路径的全限定名
	public static List<String> loadFactoryNames(Class<?> factoryClass, @Nullable ClassLoader classLoader) {
        //获取名称
	    String factoryClassName = factoryClass.getName();
        //加载并获取所有META-INF/spring.factories中的value
		return loadSpringFactories(classLoader).getOrDefault(factoryClassName, Collections.emptyList());
	}

	private static Map<String, List<String>> loadSpringFactories(@Nullable ClassLoader classLoader) {
        //根据类加载器从缓存中获取，如果缓存中存在,就直接返回，如果不存在就去加载
	    MultiValueMap<String, String> result = cache.get(classLoader);
		if (result != null) {
			return result;
		}

		try {
		    // 下面几组方法都是从类的加载路径中查找指定名称的资源，区别是查找的范围可能不一样，一般getResource/getResources不仅会查找当前类加载器的加载路径，还会迭代查找父加载器(不是父类，加载器都有一个属性指向父加载器)的加载路径
            // java.lang.ClassLoader#getBootstrapResource  这个是一个静态方法
            // java.lang.ClassLoader#getBootstrapResources 这个是一个静态方法
            // java.lang.ClassLoader#getSystemResource  这个是一个静态方法
            // 用于从加载类的搜索路径中查找指定名称的资源，找到第一个就返回
            // java.lang.ClassLoader#getSystemResources  这个是一个静态方法
            // 用于从加载类的搜索路径中查找指定名称的所有资源。
            // java.lang.ClassLoader#getResource () 
            // 会返回类路径上碰到的第一个资源。
            // java.lang.ClassLoader#getResources
            // 返回当前类加载器路径上的所有重复资源以及父类加载器上的所有重复资源。

            //获取所有JAR及classpath路径下的META-INF/spring.factories的路径
			Enumeration<URL> urls = (classLoader != null ?
					classLoader.getResources(FACTORIES_RESOURCE_LOCATION) :
					ClassLoader.getSystemResources(FACTORIES_RESOURCE_LOCATION));
			result = new LinkedMultiValueMap<>();
            //遍历所有的META-INF/spring.factories的路径
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				UrlResource resource = new UrlResource(url);
                //将META-INF/spring.factories中的key value加载为Prpperties对象
				Properties properties = PropertiesLoaderUtils.loadProperties(resource);
				for (Map.Entry<?, ?> entry : properties.entrySet()) {
					List<String> factoryClassNames = Arrays.asList(
							StringUtils.commaDelimitedListToStringArray((String) entry.getValue()));
					//将文件中的key-value添加进result
					result.addAll((String) entry.getKey(), factoryClassNames);
				}
			}
            //放入到缓存中
			cache.put(classLoader, result);
			return result;
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Unable to load factories from location [" +
					FACTORIES_RESOURCE_LOCATION + "]", ex);
		}
	}

	@SuppressWarnings("unchecked")
    //实例化Bean对象
	private static <T> T instantiateFactory(String instanceClassName, Class<T> factoryClass, ClassLoader classLoader) {
		try {
			Class<?> instanceClass = ClassUtils.forName(instanceClassName, classLoader);
			if (!factoryClass.isAssignableFrom(instanceClass)) {
				throw new IllegalArgumentException(
						"Class [" + instanceClassName + "] is not assignable to [" + factoryClass.getName() + "]");
			}
			return (T) ReflectionUtils.accessibleConstructor(instanceClass).newInstance();
		}
		catch (Throwable ex) {
			throw new IllegalArgumentException("Unable to instantiate factory class: " + factoryClass.getName(), ex);
		}
	}

}

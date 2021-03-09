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

package org.springframework.aop.aspectj.annotation;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.reflect.PerClauseKind;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJProxyUtils;
import org.springframework.aop.aspectj.SimpleAspectInstanceFactory;
import org.springframework.aop.framework.ProxyCreatorSupport;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * AspectJ-based proxy factory, allowing for programmatic building
 * of proxies which include AspectJ aspects (code style as well
 * Java 5 annotation style).
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @since 2.0
 * @see #addAspect(Object)
 * @see #addAspect(Class)
 * @see #getProxy()
 * @see #getProxy(ClassLoader)
 * @see org.springframework.aop.framework.ProxyFactory
 */
@SuppressWarnings("serial")

// Advised这个接口的实现类封装了Interceptor(属于advice)，其他advice,  Adivisors ,代理的接口，
// 由AdvisedSupport实现，直接实现类也只有AdvisedSupport
/**
 * 是生成代理的一个重要的接口定义, 继承关系如下：
 * ===============================begin=============================================
 * AdvisedSupport继承了ProxyConfig类实现了Advised接口。如果你去翻看这两个类的代码的话，
 * 会发现在Advised中定义了一些列的方法，而在ProxyConfig中是对这些接口方法的一个实现，
 * 但是Advised和ProxyConfig却是互相独立的两个类。但是SpringAOP通过AdvisedSupport将他们适配到了一起。
 * AdvisedSupport只有一个子类，这个子类就是ProxyCreatorSupport。从这两个类的名字我们可以看到他们的作用：
 * 一个是为Advised提供支持的类，一个是为代理对象的创建提供支持的类。
 *                                                                                            <==  AspectJProxyFactory
 * TargetClassAware <== Advised & ProxyConfig <==  AdvisedSupport  <==  ProxyCreatorSupport   <==  ProxyFactoryBean
 *                                                                                            <==  ProxyFactory
 * //我们这个例子anothersample1使用AspectJProxyFactory.getProxy创建代理对象以及拦截器链Advisors
 * //Spring 使用的 ProxyFactory 创建代理对象以及拦截器链Advisors
 * //创建代理对象时，targetsource 和advisors 被封装在 ProxyFactory/AspectJProxyFactory(或其父类中)
 * //所以，一个target需要new 一个  ProxyFactory/AspectJProxyFactory来创建代理
 * ===============================end  =============================================
 */
public class AspectJProxyFactory extends ProxyCreatorSupport {

	/** Cache for singleton aspect instances */
	private static final Map<Class<?>, Object> aspectCache = new ConcurrentHashMap<>();

	private final AspectJAdvisorFactory aspectFactory = new ReflectiveAspectJAdvisorFactory();


	/**
	 * Create a new AspectJProxyFactory.
	 */
	public AspectJProxyFactory() {
	}

	/**
	 * Create a new AspectJProxyFactory.
	 * <p>Will proxy all interfaces that the given target implements.
	 * @param target the target object to be proxied
	 */
	//当我们调用AspectJProxyFactory的有参构造函数时，它做了这几件事，检测目标对象不能为null，设置目标对象的所有的接口，设置目标对象。
    //获取类上的所有的接口是通过调用ClassUtils.getAllInterfaces来获取的。这个方法可以获取类上的所有接口，包括父类上的接口，但是它不能获取接口的接口。
	public AspectJProxyFactory(Object target) {
		Assert.notNull(target, "Target object must not be null");
		setInterfaces(ClassUtils.getAllInterfaces(target));
		setTarget(target);
	}

	/**
	 * Create a new {@code AspectJProxyFactory}.
	 * No target, only interfaces. Must add interceptors.
	 */
	public AspectJProxyFactory(Class<?>... interfaces) {
		setInterfaces(interfaces);
	}


	/**
	 * Add the supplied aspect instance to the chain. The type of the aspect instance
	 * supplied must be a singleton aspect. True singleton lifecycle is not honoured when
	 * using this method - the caller is responsible for managing the lifecycle of any
	 * aspects added in this way.
	 * @param aspectInstance the AspectJ aspect instance
	 */
    //添加切面,入参是切面实例对象
	public void addAspect(Object aspectInstance) {
		Class<?> aspectClass = aspectInstance.getClass();
		String aspectName = aspectClass.getName();
		AspectMetadata am = createAspectMetadata(aspectClass, aspectName);
		if (am.getAjType().getPerClause().getKind() != PerClauseKind.SINGLETON) {
			throw new IllegalArgumentException(
					"Aspect class [" + aspectClass.getName() + "] does not define a singleton aspect");
		}
		addAdvisorsFromAspectInstanceFactory(
				new SingletonMetadataAwareAspectInstanceFactory(aspectInstance, aspectName));
	}

	/**
	 * Add an aspect of the supplied type to the end of the advice chain.
	 * @param aspectClass the AspectJ aspect class
	 */
	//添加切面,个入参是切面类对象
	public void addAspect(Class<?> aspectClass) {
        //全限定类名
		String aspectName = aspectClass.getName();
        //根据切面对象创建切面元数据类
		AspectMetadata am = createAspectMetadata(aspectClass, aspectName);
        //根据传入的切面类创建 切面实例 将切面实例封装为切面实例工厂
		MetadataAwareAspectInstanceFactory instanceFactory = createAspectInstanceFactory(am, aspectClass, aspectName);
        //从切面实例工厂中获取Advisor
		addAdvisorsFromAspectInstanceFactory(instanceFactory);
	}


	/**
	 * Add all {@link Advisor Advisors} from the supplied {@link MetadataAwareAspectInstanceFactory}
	 * to the current chain. Exposes any special purpose {@link Advisor Advisors} if needed.
	 * @see AspectJProxyUtils#makeAdvisorChainAspectJCapableIfNecessary(List)
	 */
	private void addAdvisorsFromAspectInstanceFactory(MetadataAwareAspectInstanceFactory instanceFactory) {
        //获取Advisor的过程我们在之前分析了
	    List<Advisor> advisors = this.aspectFactory.getAdvisors(instanceFactory);
		Class<?> targetClass = getTargetClass();
		Assert.state(targetClass != null, "Unresolvable target class");
        //这句代码的意思是为我们的目标类挑选合适的Advisor
		advisors = AopUtils.findAdvisorsThatCanApply(advisors, targetClass);
		AspectJProxyUtils.makeAdvisorChainAspectJCapableIfNecessary(advisors);
        //为Advisor进行排序
		AnnotationAwareOrderComparator.sort(advisors);
		addAdvisors(advisors);
	}

	/**
	 * Create an {@link AspectMetadata} instance for the supplied aspect type.
	 */
	private AspectMetadata createAspectMetadata(Class<?> aspectClass, String aspectName) {
        //直接调用 AspectMetadata的构造函数  创建对象 入参为：切面类和切面类的全限定类名
	    AspectMetadata am = new AspectMetadata(aspectClass, aspectName);
        //如果切面类不是切面则抛出异常
        //这里判断我们传入的切面类是不是切面很简单，即判断切面类上是否存在@Aspect注解。
        //这里判断一个类是不是切面类是这样进行判断的：如果我们传入的切面类上没有@Aspect注解的话，则去查找它的父类上
        //是否存在@Aspect注解。一直查到父类为Object。如果一直没有找到带有@Aspect注解的类，则会抛出异常。
		if (!am.getAjType().isAspect()) {
			throw new IllegalArgumentException("Class [" + aspectClass.getName() + "] is not a valid aspect type");
		}
		return am;
	}

	/**
	 * Create a {@link MetadataAwareAspectInstanceFactory} for the supplied aspect type. If the aspect type
	 * has no per clause, then a {@link SingletonMetadataAwareAspectInstanceFactory} is returned, otherwise
	 * a {@link PrototypeAspectInstanceFactory} is returned.
	 */
	private MetadataAwareAspectInstanceFactory createAspectInstanceFactory(
			AspectMetadata am, Class<?> aspectClass, String aspectName) {

		MetadataAwareAspectInstanceFactory instanceFactory;
		//PerClauseKind.SINGLETON 的情况，我们基本都用这个
		if (am.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
			// Create a shared aspect instance.
            //根据传入的切面类创建 切面对象 是一个单例 要求有无参构造函数
            //这个获取 单例 切面对象的方式可以学习一下
			Object instance = getSingletonAspectInstance(aspectClass);
            //将上一步创建的切面对象 封装到SingletonMetadataAwareAspectInstanceFactory中
            //从名字我们也可以看出来 这是一个单例的带有切面元数据的切面实例工厂
			instanceFactory = new SingletonMetadataAwareAspectInstanceFactory(instance, aspectName);
		}
		else {
			// Create a factory for independent aspect instances.
            //这里创建一个 SimpleMetadataAwareAspectInstanceFactory 传入切面类和切面名字
			instanceFactory = new SimpleMetadataAwareAspectInstanceFactory(aspectClass, aspectName);
		}
		return instanceFactory;
	}

	/**
	 * Get the singleton aspect instance for the supplied aspect type. An instance
	 * is created if one cannot be found in the instance cache.
	 */
	private Object getSingletonAspectInstance(Class<?> aspectClass) {
		// Quick check without a lock...
		Object instance = aspectCache.get(aspectClass);
		if (instance == null) {
			synchronized (aspectCache) {
				// To be safe, check within full lock now...
				instance = aspectCache.get(aspectClass);
				if (instance == null) {
					instance = new SimpleAspectInstanceFactory(aspectClass).getAspectInstance();
					aspectCache.put(aspectClass, instance);
				}
			}
		}
		return instance;
	}


	/**
	 * Create a new proxy according to the settings in this factory.
	 * <p>Can be called repeatedly. Effect will vary if we've added
	 * or removed interfaces. Can add and remove interceptors.
	 * <p>Uses a default class loader: Usually, the thread context class loader
	 * (if necessary for proxy creation).
	 * @return the new proxy
	 */
	@SuppressWarnings("unchecked")
	public <T> T getProxy() {
		return (T) createAopProxy().getProxy();
	}

	/**
	 * Create a new proxy according to the settings in this factory.
	 * <p>Can be called repeatedly. Effect will vary if we've added
	 * or removed interfaces. Can add and remove interceptors.
	 * <p>Uses the given class loader (if necessary for proxy creation).
	 * @param classLoader the class loader to create the proxy with
	 * @return the new proxy
	 */
	@SuppressWarnings("unchecked")
	public <T> T getProxy(ClassLoader classLoader) {
		return (T) createAopProxy().getProxy(classLoader);
	}

}

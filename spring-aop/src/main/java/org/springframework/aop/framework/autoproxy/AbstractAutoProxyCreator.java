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

package org.springframework.aop.framework.autoproxy;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.TargetSource;
import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.ProxyProcessorSupport;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} implementation
 * that wraps each eligible bean with an AOP proxy, delegating to specified interceptors
 * before invoking the bean itself.
 *
 * <p>This class distinguishes between "common" interceptors: shared for all proxies it
 * creates, and "specific" interceptors: unique per bean instance. There need not be any
 * common interceptors. If there are, they are set using the interceptorNames property.
 * As with {@link org.springframework.aop.framework.ProxyFactoryBean}, interceptors names
 * in the current factory are used rather than bean references to allow correct handling
 * of prototype advisors and interceptors: for example, to support stateful mixins.
 * Any advice type is supported for {@link #setInterceptorNames "interceptorNames"} entries.
 *
 * <p>Such auto-proxying is particularly useful if there's a large number of beans that
 * need to be wrapped with similar proxies, i.e. delegating to the same interceptors.
 * Instead of x repetitive proxy definitions for x target beans, you can register
 * one single such post processor with the bean factory to achieve the same effect.
 *
 * <p>Subclasses can apply any strategy to decide if a bean is to be proxied, e.g. by type,
 * by name, by definition details, etc. They can also return additional interceptors that
 * should just be applied to the specific bean instance. A simple concrete implementation is
 * {@link BeanNameAutoProxyCreator}, identifying the beans to be proxied via given names.
 *
 * <p>Any number of {@link TargetSourceCreator} implementations can be used to create
 * a custom target source: for example, to pool prototype objects. Auto-proxying will
 * occur even if there is no advice, as long as a TargetSourceCreator specifies a custom
 * {@link org.springframework.aop.TargetSource}. If there are no TargetSourceCreators set,
 * or if none matches, a {@link org.springframework.aop.target.SingletonTargetSource}
 * will be used by default to wrap the target bean instance.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Rob Harrop
 * @since 13.10.2003
 * @see #setInterceptorNames
 * @see #getAdvicesAndAdvisorsForBean
 * @see BeanNameAutoProxyCreator
 * @see DefaultAdvisorAutoProxyCreator
 */
@SuppressWarnings("serial")
//这个是创建代理的核心抽象类，有不同的实现，我们可以使用任何一种实现来创建代理，不过得按照对应的用法，怎么用在具体实现类里面说明
/**
 * AbstractAutoProxyCreator有两个子类
 *
 * 第二类(一个)是BeanNameAutoProxyCreator，它是基于bean名字的自动代理类。 它会给spring容器中bean名字与指定名字匹配的bean自动创建代理。其中匹配的规则定义在PatternMatchUtils.simpleMatch()方法中。
 * 注意：若需要给某个FactoryBean创建代理，可以在bean名字前面加上&.
 *
 * 第二类是AbstractAdvisorAutoProxyCreator，相对BeanNameAutoProxyCreator而言，它更为强大，它会自动获取spring容器中注册的所有的Advisor类（除了子类中isEligibleAdvisorBean（）方法指定的不满足条件的Advisor除外。），
 * 然后自动给spring容器中满足Advisor中pointCut创建代理。
 *      DefaultAdvisorAutoProxyCreator是默认实现，默认会自动代理所有的Advisor，当然也可以通过设置usePrefix和advisorBeanNamePrefix来过滤部分advisor
 *      AspectJAwareAdvisorAutoProxyCreator用于支持AspectJ方式(使用AspectJ标签或者@AspectJ注解)的自动代理。(子类为AnnotationAwareAspectJAutoProxyCreator，处理注解@AspectJ语法)
 */
public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport
		implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {

	/**
	 * Convenience constant for subclasses: Return value for "do not proxy".
	 * @see #getAdvicesAndAdvisorsForBean
	 */
	@Nullable
    //为null的final常量，getAdvicesAndAdvisorsForBean返回DO_NOT_PROXY的话就表示没有代理逻辑，所以也不需要代理
	protected static final Object[] DO_NOT_PROXY = null;

	/**
	 * Convenience constant for subclasses: Return value for
	 * "proxy without additional interceptors, just the common ones".
	 * @see #getAdvicesAndAdvisorsForBean
	 */
	//用在BeanNameAutoProxyCreator.getAdvicesAndAdvisorsForBean中
	//为null的final常量，BeanNameAutoProxyCreator.getAdvicesAndAdvisorsForBean返回PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS标示传入的bean是需要被代理的，(对于BeanNameAutoProxyCreator来说，bean名称在配置的名称列表)
	protected static final Object[] PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS = new Object[0];


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** Default is global AdvisorAdapterRegistry */
	private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

	/**
	 * Indicates whether or not the proxy should be frozen. Overridden from super to prevent the configuration from becoming frozen too early.
	 */
	private boolean freezeProxy = false;

	/** Default is no common interceptors */
	private String[] interceptorNames = new String[0];

	private boolean applyCommonInterceptorsFirst = true;

	@Nullable
	private TargetSourceCreator[] customTargetSourceCreators;

	@Nullable
	private BeanFactory beanFactory;

	private final Set<String> targetSourcedBeans = Collections.newSetFromMap(new ConcurrentHashMap<>(16));

	private final Set<Object> earlyProxyReferences = Collections.newSetFromMap(new ConcurrentHashMap<>(16));

	private final Map<Object, Class<?>> proxyTypes = new ConcurrentHashMap<>(16);

	//记录Object是否需要被代理，bean和boolean映射的集合
	private final Map<Object, Boolean> advisedBeans = new ConcurrentHashMap<>(256);


	/**
	 * Set whether or not the proxy should be frozen, preventing advice
	 * from being added to it once it is created.
	 * <p>Overridden from the super class to prevent the proxy configuration
	 * from being frozen before the proxy is created.
	 */
	@Override
	public void setFrozen(boolean frozen) {
		this.freezeProxy = frozen;
	}

	@Override
	public boolean isFrozen() {
		return this.freezeProxy;
	}

	/**
	 * Specify the {@link AdvisorAdapterRegistry} to use.
	 * <p>Default is the global {@link AdvisorAdapterRegistry}.
	 * @see org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry
	 */
	public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
		this.advisorAdapterRegistry = advisorAdapterRegistry;
	}

	/**
	 * Set custom {@code TargetSourceCreators} to be applied in this order.
	 * If the list is empty, or they all return null, a {@link SingletonTargetSource}
	 * will be created for each bean.
	 * <p>Note that TargetSourceCreators will kick in even for target beans
	 * where no advices or advisors have been found. If a {@code TargetSourceCreator}
	 * returns a {@link TargetSource} for a specific bean, that bean will be proxied
	 * in any case.
	 * <p>{@code TargetSourceCreators} can only be invoked if this post processor is used
	 * in a {@link BeanFactory} and its {@link BeanFactoryAware} callback is triggered.
	 * @param targetSourceCreators the list of {@code TargetSourceCreators}.
	 * Ordering is significant: The {@code TargetSource} returned from the first matching
	 * {@code TargetSourceCreator} (that is, the first that returns non-null) will be used.
	 */
	public void setCustomTargetSourceCreators(TargetSourceCreator... targetSourceCreators) {
		this.customTargetSourceCreators = targetSourceCreators;
	}

	/**
	 * Set the common interceptors. These must be bean names in the current factory.
	 * They can be of any advice or advisor type Spring supports.
	 * <p>If this property isn't set, there will be zero common interceptors.
	 * This is perfectly valid, if "specific" interceptors such as matching
	 * Advisors are all we want.
	 */
	public void setInterceptorNames(String... interceptorNames) {
		this.interceptorNames = interceptorNames;
	}

	/**
	 * Set whether the common interceptors should be applied before bean-specific ones.
	 * Default is "true"; else, bean-specific interceptors will get applied first.
	 */
	public void setApplyCommonInterceptorsFirst(boolean applyCommonInterceptorsFirst) {
		this.applyCommonInterceptorsFirst = applyCommonInterceptorsFirst;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * Return the owning {@link BeanFactory}.
	 * May be {@code null}, as this post-processor doesn't need to belong to a bean factory.
	 */
	@Nullable
	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}


	@Override
	@Nullable
    //如果bean继承了FactoryBean，那么根据传入的名字，判断要返回的类型
	public Class<?> predictBeanType(Class<?> beanClass, String beanName) {
		if (this.proxyTypes.isEmpty()) {
			return null;
		}
		Object cacheKey = getCacheKey(beanClass, beanName);
		return this.proxyTypes.get(cacheKey);
	}

	@Override
	@Nullable
	public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	@Override
	public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
		Object cacheKey = getCacheKey(bean.getClass(), beanName);
		if (!this.earlyProxyReferences.contains(cacheKey)) {
			this.earlyProxyReferences.add(cacheKey);
		}
		return wrapIfNecessary(bean, beanName, cacheKey);
	}

	@Override
    //这个方法可以让我们在Bean被Spring容器实例化之前提前创建Bean，如果这个方法返回的值不是null，那就中断其他类对这个接口的实现，直接返回这个创建的Bean。

    //如果用户提供了targetSource的话，就是用这个Before方法创建一个代理对象；
    //并且查找bean Facotry中的Class得到所有切面Advisor(被@Aspect注解的bean), 然后根据切面中的信息(切点+连接点+通知)，
    //判断当前bean是否应该被代理，把当前bean对应的class和是否应该被代理的映射保存在AbstractAutoProxyCreator#advisedBeans中
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        //得到一个缓存的key 实现是：如果是beanClass是FactoryBean类型，则在beanName前面加&
	    Object cacheKey = getCacheKey(beanClass, beanName);

	    //如果目标Bean的集合中包这个beanName的话， 则跳过，
		//否则将bean与这个bean是否应该被代理之间的映射放进advisedBeans里面
		if (!StringUtils.hasLength(beanName) || !this.targetSourcedBeans.contains(beanName)) {
			if (this.advisedBeans.containsKey(cacheKey)) {
				return null;
			}
            //AnnotationAwareAspectJAutoProxyCreator重写了isInfrastructureClass()方法，在原来的基础上添加 ： 类上面有@Aspect注解的也不能被代理
            //isInfrastructureClass 判断是不是负责AOP基础建设的Bean，如果是AOP基础建设的Bean不能在这里被创建代理对象，
            //那么什么样的Bean是AOP的基础建设Bean呢？Advice、Pointcut、Advisor、AopInfrastructureBean类型的Bean，以及类上面有@Aspect注解的()
            //所以如果我们不想一个类被代理的话，可以实现AopInfrastructureBean接口，AopInfrastructureBean接口的功能就是这个，

            //shouldSkip ：判断一个bean是否应该跳过，不创建代理，需要查找所有的Advisor 并且或进行缓存
            //如果Advisor是AbstractAspectJAdvice类型并且aspectName和beanName相等则跳过
            if (isInfrastructureClass(beanClass) || shouldSkip(beanClass, beanName)) {
				this.advisedBeans.put(cacheKey, Boolean.FALSE);
				return null;
			}
		}

		// Create proxy here if we have a custom TargetSource.
		// Suppresses unnecessary default instantiation of the target bean:
		// The TargetSource will handle target instances in a custom fashion.
		TargetSource targetSource = getCustomTargetSource(beanClass, beanName);
		if (targetSource != null) {
			if (StringUtils.hasLength(beanName)) {
				this.targetSourcedBeans.add(beanName);
			}

            /**
             * getAdvicesAndAdvisorsForBean()用于获取Advisors,不同的子类有不同的实现：
             * 1. 继承AbstractAdvisorAutoProxyCreator的类,比如AnnotationAwareAspectJAutoProxyCreator
             *    AbstractAutowireCapableBeanFactory#doCreateBean
             *    ==>AbstractAutowireCapableBeanFactory#initializeBean
             *    ==>AbstractAutowireCapableBeanFactory#applyBeanPostProcessorsAfterInitialization
             *    ==>AbstractAutoProxyCreator#postProcessAfterInitialization
             *    ==>AbstractAutoProxyCreator#getAdvicesAndAdvisorsForBean
             *    ==>AbstractAdvisorAutoProxyCreator#getAdvicesAndAdvisorsForBean
             *    ==>AbstractAdvisorAutoProxyCreator#findEligibleAdvisors
             *    ==>AnnotationAwareAspectJAutoProxyCreator#findCandidateAdvisors
             *    ==>BeanFactoryAspectJAdvisorsBuilder#buildAspectJAdvisors  走到这个方法里面获取Advisors，
             *    之前的流程其实调用过buildAspectJAdvisors(AspectJAwareAdvisorAutoProxyCreator#shouldSkip->buildAspectJAdvisors)，
             *    这里直接走buildAspectJAdvisors的缓存if分支，从缓存中直接获取
             * 2. BeanNameAutoProxyCreator#getAdvicesAndAdvisorsForBean()
             *    为每个beanName配置对应的Advisors, 然后这个方法根据beanName获取，具体看BeanNameAutoProxyCreator这个类的注释demo
             */
			Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);

			//!!!!!!!!!!  使用这行代码调用createProxy(), 里面使用ProxyFactory创建代理     !!!!!!!!!!
			Object proxy = createProxy(beanClass, beanName, specificInterceptors, targetSource);
			this.proxyTypes.put(cacheKey, proxy.getClass());
			return proxy;
		}

		return null;
	}

	@Override
	public boolean postProcessAfterInstantiation(Object bean, String beanName) {
		return true;
	}

	@Override
	public PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) {

		return pvs;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) {
		return bean;
	}

	/**
	 * Create a proxy with the configured interceptors if the bean is
	 * identified as one to proxy by the subclass.
	 * @see #getAdvicesAndAdvisorsForBean
	 */
	@Override
    //用户没有提供targetSource的话，走这个流程用这个After方法创建代理。使用配置的拦截器创建代理
	public Object postProcessAfterInitialization(@Nullable Object bean, String beanName) throws BeansException {
		if (bean != null) {
            //获取缓存的key
			Object cacheKey = getCacheKey(bean.getClass(), beanName);
            //如果不是提前引用的Bean
			if (!this.earlyProxyReferences.contains(cacheKey)) {
                //创建代理
				return wrapIfNecessary(bean, beanName, cacheKey);
			}
		}
		return bean;
	}


	/**
	 * Build a cache key for the given bean class and bean name.
	 * <p>Note: As of 4.2.3, this implementation does not return a concatenated
	 * class/name String anymore but rather the most efficient cache key possible:
	 * a plain bean name, prepended with {@link BeanFactory#FACTORY_BEAN_PREFIX}
	 * in case of a {@code FactoryBean}; or if no bean name specified, then the
	 * given bean {@code Class} as-is.
	 * @param beanClass the bean class
	 * @param beanName the bean name
	 * @return the cache key for the given class and name
	 */
	protected Object getCacheKey(Class<?> beanClass, @Nullable String beanName) {
		if (StringUtils.hasLength(beanName)) {
			return (FactoryBean.class.isAssignableFrom(beanClass) ?
					BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName);
		}
		else {
			return beanClass;
		}
	}

	/**
	 * Wrap the given bean if necessary, i.e. if it is eligible for being proxied.
	 * @param bean the raw bean instance
	 * @param beanName the name of the bean
	 * @param cacheKey the cache key for metadata access
	 * @return a proxy wrapping the bean, or the raw bean instance as-is
	 */
	//这是Spring实现Bean代理的核心方法，也是创建代理的入口
    //wrapIfNecessary在两处会被调用，一处是getEarlyBeanReference，另一处是postProcessAfterInitialization。
	protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
		if (StringUtils.hasLength(beanName) && this.targetSourcedBeans.contains(beanName)) {
			return bean;
		}

		// 判断是否不应该代理这个bean
		if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
			return bean;
		}

        /*
         * 判断是否是一些InfrastructureClass或者是否应该跳过这个bean。
         * 所谓InfrastructureClass就是指Advice/PointCut/Advisor等接口的实现类。
         * shouldSkip默认实现为返回false,由于是protected方法，子类可以覆盖。
         */
		if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
			this.advisedBeans.put(cacheKey, Boolean.FALSE);
			return bean;
		}

		// Create proxy if we have advice.

        /**
         * getAdvicesAndAdvisorsForBean()用于获取Advisors,不同的子类有不同的实现：
         * 1. 继承AbstractAdvisorAutoProxyCreator的类,比如AnnotationAwareAspectJAutoProxyCreator
         *    AbstractAutowireCapableBeanFactory#doCreateBean
         *    ==>AbstractAutowireCapableBeanFactory#initializeBean
         *    ==>AbstractAutowireCapableBeanFactory#applyBeanPostProcessorsAfterInitialization
         *    ==>AbstractAutoProxyCreator#postProcessAfterInitialization
         *    ==>AbstractAutoProxyCreator#getAdvicesAndAdvisorsForBean
         *    ==>AbstractAdvisorAutoProxyCreator#getAdvicesAndAdvisorsForBean
         *    ==>AbstractAdvisorAutoProxyCreator#findEligibleAdvisors
         *    ==>AnnotationAwareAspectJAutoProxyCreator#findCandidateAdvisors
         *    ==>BeanFactoryAspectJAdvisorsBuilder#buildAspectJAdvisors  走到这个方法里面获取Advisors，
         *    之前的流程其实调用过buildAspectJAdvisors(AspectJAwareAdvisorAutoProxyCreator#shouldSkip->buildAspectJAdvisors)，
         *    这里直接走buildAspectJAdvisors的缓存if分支，从缓存中直接获取
         * 2. BeanNameAutoProxyCreator#getAdvicesAndAdvisorsForBean()
         *    为每个beanName配置对应的Advisors, 然后这个方法根据beanName获取，具体看BeanNameAutoProxyCreator这个类的注释demo
         */
		Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
		if (specificInterceptors != DO_NOT_PROXY) {

            // 将cacheKey添加到已经被增强列表，防止多次增强
			this.advisedBeans.put(cacheKey, Boolean.TRUE);

            //!!!!!!!!!!  使用这行代码调用createProxy(), 里面使用ProxyFactory创建代理     !!!!!!!!!!
			Object proxy = createProxy(
					bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));

            // 缓存代理类型
			this.proxyTypes.put(cacheKey, proxy.getClass());
			return proxy;
		}

        // 这个bean不需要被代理
		this.advisedBeans.put(cacheKey, Boolean.FALSE);
		return bean;
	}

	/**
	 * Return whether the given bean class represents an infrastructure class that should never be proxied.
	 * <p>The default implementation considers Advices, Advisors and
	 * AopInfrastructureBeans as infrastructure classes.
	 * @param beanClass the class of the bean
	 * @return whether the bean represents an infrastructure class
	 * @see org.aopalliance.aop.Advice
	 * @see org.springframework.aop.Advisor
	 * @see org.springframework.aop.framework.AopInfrastructureBean
	 * @see #shouldSkip
	 */
	// 返回给定的bean类是否代表不应该被代理的基础架构类
    // 这里的基础架构类包括Advice  Pointcut Advisor AopInfrastructureBean
	protected boolean isInfrastructureClass(Class<?> beanClass) {
		boolean retVal = Advice.class.isAssignableFrom(beanClass) ||
				Pointcut.class.isAssignableFrom(beanClass) ||
				Advisor.class.isAssignableFrom(beanClass) ||
				AopInfrastructureBean.class.isAssignableFrom(beanClass);
		if (retVal && logger.isTraceEnabled()) {
			logger.trace("Did not attempt to auto-proxy infrastructure class [" + beanClass.getName() + "]");
		}
		return retVal;
	}

	/**
	 * Subclasses should override this method to return {@code true} if the
	 * given bean should not be considered for auto-proxying by this post-processor.
	 * <p>Sometimes we need to be able to avoid this happening if it will lead to
	 * a circular reference. This implementation returns {@code false}.
	 * @param beanClass the class of the bean
	 * @param beanName the name of the bean
	 * @return whether to skip the given bean
	 */
	protected boolean shouldSkip(Class<?> beanClass, String beanName) {
		return false;
	}

	/**
	 * Create a target source for bean instances. Uses any TargetSourceCreators if set.
	 * Returns {@code null} if no custom TargetSource should be used.
	 * <p>This implementation uses the "customTargetSourceCreators" property.
	 * Subclasses can override this method to use a different mechanism.
	 * @param beanClass the class of the bean to create a TargetSource for
	 * @param beanName the name of the bean
	 * @return a TargetSource for this bean
	 * @see #setCustomTargetSourceCreators
	 */
	@Nullable
	protected TargetSource getCustomTargetSource(Class<?> beanClass, String beanName) {
		// We can't create fancy target sources for directly registered singletons.
		if (this.customTargetSourceCreators != null &&
				this.beanFactory != null && this.beanFactory.containsBean(beanName)) {
			for (TargetSourceCreator tsc : this.customTargetSourceCreators) {
				TargetSource ts = tsc.getTargetSource(beanClass, beanName);
				if (ts != null) {
					// Found a matching TargetSource.
					if (logger.isDebugEnabled()) {
						logger.debug("TargetSourceCreator [" + tsc +
								"] found custom TargetSource for bean with name '" + beanName + "'");
					}
					return ts;
				}
			}
		}

		// No custom TargetSource found.
		return null;
	}

	/**
	 * Create an AOP proxy for the given bean.
	 * @param beanClass the class of the bean
	 * @param beanName the name of the bean
	 * @param specificInterceptors the set of interceptors that is
	 * specific to this bean (may be empty, but not null)
	 * @param targetSource the TargetSource for the proxy,
	 * already pre-configured to access the bean
	 * @return the AOP proxy for the bean
	 * @see #buildAdvisors
	 */
	//不管是用户有没有自己提供targetsource都会走到这个方法创建代理对象
    //这个方法里面使用CglibAopProxy或者JdkDynamicAopProxy创建对象
	protected Object createProxy(Class<?> beanClass, @Nullable String beanName,
			@Nullable Object[] specificInterceptors, TargetSource targetSource) {

		if (this.beanFactory instanceof ConfigurableListableBeanFactory) {
			AutoProxyUtils.exposeTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName, beanClass);
		}

        /**
         * 是生成代理的一个重要的接口定义, 继承关系如下：
         * ===============================begin=============================================
         * AdvisedSupport继承了ProxyConfig类实现了Advised接口。如果你去翻看这两个类的代码的话，
         * 会发现在Advised中定义了一些列的方法，而在ProxyConfig中是对这些接口方法的一个实现，
         * 但是Advised和ProxyConfig却是互相独立的两个类。但是SpringAOP通过AdvisedSupport将他们适配到了一
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
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.copyFrom(this);

		//使用jdk创建代理的话要给ProxyFactory配置AdvisedSupport.interfaces
        //使用cglib创建代理的话要给ProxyFactory配置ProxyConfig.proxyTargetClass
		if (!proxyFactory.isProxyTargetClass()) {
			//使用cglib生成动态代理对象的话设置为true
			if (shouldProxyTargetClass(beanClass, beanName)) {
				proxyFactory.setProxyTargetClass(true);
			}
			else {
				evaluateProxyInterfaces(beanClass, proxyFactory);
			}
		}


		//给ProxyFactory配置Advisors
		Advisor[] advisors = buildAdvisors(beanName, specificInterceptors);
		proxyFactory.addAdvisors(advisors);
		proxyFactory.setTargetSource(targetSource);
		customizeProxyFactory(proxyFactory);

		proxyFactory.setFrozen(this.freezeProxy);
		if (advisorsPreFiltered()) {
			proxyFactory.setPreFiltered(true);
		}

		//到这里，ProxyFactory已经创建和配置完成，我们开始创建代理对象
		return proxyFactory.getProxy(getProxyClassLoader());
	}

	/**
	 * Determine whether the given bean should be proxied with its target class rather than its interfaces.
	 * <p>Checks the {@link AutoProxyUtils#PRESERVE_TARGET_CLASS_ATTRIBUTE "preserveTargetClass" attribute}
	 * of the corresponding bean definition.
	 * @param beanClass the class of the bean
	 * @param beanName the name of the bean
	 * @return whether the given bean should be proxied with its target class
	 * @see AutoProxyUtils#shouldProxyTargetClass
	 */
	protected boolean shouldProxyTargetClass(Class<?> beanClass, @Nullable String beanName) {
		return (this.beanFactory instanceof ConfigurableListableBeanFactory &&
				AutoProxyUtils.shouldProxyTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName));
	}

	/**
	 * Return whether the Advisors returned by the subclass are pre-filtered
	 * to match the bean's target class already, allowing the ClassFilter check
	 * to be skipped when building advisors chains for AOP invocations.
	 * <p>Default is {@code false}. Subclasses may override this if they
	 * will always return pre-filtered Advisors.
	 * @return whether the Advisors are pre-filtered
	 * @see #getAdvicesAndAdvisorsForBean
	 * @see org.springframework.aop.framework.Advised#setPreFiltered
	 */
	protected boolean advisorsPreFiltered() {
		return false;
	}

	/**
	 * Determine the advisors for the given bean, including the specific interceptors
	 * as well as the common interceptor, all adapted to the Advisor interface.
	 * @param beanName the name of the bean
	 * @param specificInterceptors the set of interceptors that is
	 * specific to this bean (may be empty, but not null)
	 * @return the list of Advisors for the given bean
	 */
	protected Advisor[] buildAdvisors(@Nullable String beanName, @Nullable Object[] specificInterceptors) {
		// Handle prototypes correctly...
		Advisor[] commonInterceptors = resolveInterceptorNames();

		List<Object> allInterceptors = new ArrayList<>();
		if (specificInterceptors != null) {
			allInterceptors.addAll(Arrays.asList(specificInterceptors));
			if (commonInterceptors.length > 0) {
				if (this.applyCommonInterceptorsFirst) {
					allInterceptors.addAll(0, Arrays.asList(commonInterceptors));
				}
				else {
					allInterceptors.addAll(Arrays.asList(commonInterceptors));
				}
			}
		}
		if (logger.isDebugEnabled()) {
			int nrOfCommonInterceptors = commonInterceptors.length;
			int nrOfSpecificInterceptors = (specificInterceptors != null ? specificInterceptors.length : 0);
			logger.debug("Creating implicit proxy for bean '" + beanName + "' with " + nrOfCommonInterceptors +
					" common interceptors and " + nrOfSpecificInterceptors + " specific interceptors");
		}

		Advisor[] advisors = new Advisor[allInterceptors.size()];
		for (int i = 0; i < allInterceptors.size(); i++) {
			advisors[i] = this.advisorAdapterRegistry.wrap(allInterceptors.get(i));
		}
		return advisors;
	}

	/**
	 * Resolves the specified interceptor names to Advisor objects.
	 * @see #setInterceptorNames
	 */
	private Advisor[] resolveInterceptorNames() {
		BeanFactory bf = this.beanFactory;
		ConfigurableBeanFactory cbf = (bf instanceof ConfigurableBeanFactory ? (ConfigurableBeanFactory) bf : null);
		List<Advisor> advisors = new ArrayList<>();
		for (String beanName : this.interceptorNames) {
			if (cbf == null || !cbf.isCurrentlyInCreation(beanName)) {
				Assert.state(bf != null, "BeanFactory required for resolving interceptor names");
				Object next = bf.getBean(beanName);
				advisors.add(this.advisorAdapterRegistry.wrap(next));
			}
		}
		return advisors.toArray(new Advisor[0]);
	}

	/**
	 * Subclasses may choose to implement this: for example,
	 * to change the interfaces exposed.
	 * <p>The default implementation is empty.
	 * @param proxyFactory a ProxyFactory that is already configured with
	 * TargetSource and interfaces and will be used to create the proxy
	 * immediately after this method returns
	 */
	protected void customizeProxyFactory(ProxyFactory proxyFactory) {
	}


	/**
	 * Return whether the given bean is to be proxied, what additional advices (e.g. AOP Alliance interceptors) and advisors to apply.
	 * @param beanClass the class of the bean to advise
	 * @param beanName the name of the bean
	 * @param customTargetSource the TargetSource returned by the
	 * {@link #getCustomTargetSource} method: may be ignored.
	 * Will be {@code null} if no custom target source is in use.
	 * @return an array of additional interceptors for the particular bean;
	 * or an empty array if no additional interceptors but just the common ones;
	 * or {@code null} if no proxy at all, not even with the common interceptors.
	 * See constants DO_NOT_PROXY and PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS.
	 * @throws BeansException in case of errors
	 * @see #DO_NOT_PROXY
	 * @see #PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS
	 */
	@Nullable
    //返回bean匹配到的advices (e.g. AOP Alliance interceptors) and advisors，如果返回AbstractAutoProxyCreator.DO_NOT_PROXY(一个为null的final常量)，就表示没有代理逻辑，不需要代理
    //参数如下 ：
    //beanClass : 指定的bean对应的类
    //beanName : 指定的beanName
    //customTargetSource : 用户传的TargetSource(如果有的话)
	protected abstract Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName,
			@Nullable TargetSource customTargetSource) throws BeansException;

}

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.reflect.PerClauseKind;

import org.springframework.aop.Advisor;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Helper for retrieving @AspectJ beans from a BeanFactory and building
 * Spring Advisors based on them, for use with auto-proxying.
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see AnnotationAwareAspectJAutoProxyCreator
 */
public class BeanFactoryAspectJAdvisorsBuilder {

	private final ListableBeanFactory beanFactory;

	private final AspectJAdvisorFactory advisorFactory;


	//==========================缓存Advisors等信息，不用每次都重新生成  begin===========================================
    /**
     * AbstractAutowireCapableBeanFactory#createBean(String, RootBeanDefinition, Object[])
     * ==>AbstractAutowireCapableBeanFactory#resolveBeforeInstantiation
     * ==>AbstractAutowireCapableBeanFactory#applyBeanPostProcessorsBeforeInstantiation
     * ==>AbstractAutoProxyCreator(AnnotationAwareAspectJAutoProxyCreator)#postProcessBeforeInstantiation
     * ==>AspectJAwareAdvisorAutoProxyCreator#shouldSkip
     * 目的是返回true/false, 但是需要根据有没有Advisors判断，所以顺便把Advisors缓存到BeanFactoryAspectJAdvisorsBuilder#advisorsCache，
     * 把@Aspect注解的bean name缓存到BeanFactoryAspectJAdvisorsBuilder#aspectBeanNames，
     * 把new BeanFactoryAspectInstanceFactory(一个切面对应一个MetadataAwareAspectInstanceFactory/BeanFactoryAspectInstanceFactory)缓存BeanFactoryAspectJAdvisorsBuilder#aspectFactoryCache,
     * 下次直接使用
     * ==>AnnotationAwareAspectJAutoProxyCreator#findCandidateAdvisors
     * ==>BeanFactoryAspectJAdvisorsBuilder#buildAspectJAdvisors
     */
	@Nullable
    //使用volatile list<String>变量保存加了@Aspect注解的bean name
	private volatile List<String> aspectBeanNames;
	//把Advisors缓存到BeanFactoryAspectJAdvisorsBuilder#advisorsCache
	private final Map<String, List<Advisor>> advisorsCache = new ConcurrentHashMap<>();
    //把new BeanFactoryAspectInstanceFactory(一个切面对应一个MetadataAwareAspectInstanceFactory/BeanFactoryAspectInstanceFactory)缓存BeanFactoryAspectJAdvisorsBuilder#aspectFactoryCache
	private final Map<String, MetadataAwareAspectInstanceFactory> aspectFactoryCache = new ConcurrentHashMap<>();
    //==========================缓存Advisors等信息，不用每次都重新生成  end===========================================

	/**
	 * Create a new BeanFactoryAspectJAdvisorsBuilder for the given BeanFactory.
	 * @param beanFactory the ListableBeanFactory to scan
	 */
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory) {
		this(beanFactory, new ReflectiveAspectJAdvisorFactory(beanFactory));
	}

	/**
	 * Create a new BeanFactoryAspectJAdvisorsBuilder for the given BeanFactory.
	 * @param beanFactory the ListableBeanFactory to scan
	 * @param advisorFactory the AspectJAdvisorFactory to build each Advisor with
	 */
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory, AspectJAdvisorFactory advisorFactory) {
		Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
		Assert.notNull(advisorFactory, "AspectJAdvisorFactory must not be null");
		this.beanFactory = beanFactory;
		this.advisorFactory = advisorFactory;
	}


	/**
	 * Look for AspectJ-annotated aspect beans in the current bean factory,
	 * and return to a list of Spring AOP Advisors representing them.
	 * <p>Creates a Spring Advisor for each AspectJ advice method.
	 * @return the list of {@link org.springframework.aop.Advisor} beans
	 * @see #isEligibleBean
	 */
	//在当前bean factory找到所有带有@Aspect注解的Class， 使用反射获取Class中Method以及注解信息，构造Advisor
	public List<Advisor> buildAspectJAdvisors() {
	    //第一次进这里aspectBeanNames是空的，然后在这个方法中给aspectBeanNames赋值
        //所有的切面的名字 这里的处理逻辑和上面的是一样的 获取到所有的切面BeanName之后缓存起来 volatile类型的
		List<String> aspectNames = this.aspectBeanNames;

		if (aspectNames == null) {
			synchronized (this) {
                //这里又赋值了一次,看着这个逻辑是不是单例模式很像
				aspectNames = this.aspectBeanNames;
				if (aspectNames == null) {
					List<Advisor> advisors = new ArrayList<>();
					aspectNames = new ArrayList<>();

					//得到beanFactory中所有的beanNames
					String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
							this.beanFactory, Object.class, true, false);
					for (String beanName : beanNames) {

					    //判断bean name是否满足设定的正则表达式，默认都会返回true
						if (!isEligibleBean(beanName)) {
							continue;
						}
						// We must be careful not to instantiate beans eagerly as in this case they
						// would be cached by the Spring container but would not have been weaved.
                        //注意看上面这个注释的内容：在这个场景下我们获取BeanClass的时候必须要小心处理，以免会提前初始化
                        //Bean，这些Bean在初始化之后会被Spring容器缓存起来，但是这些Bean可能还没有被织入。
						Class<?> beanType = this.beanFactory.getType(beanName);
						if (beanType == null) {
							continue;
						}
                        //判断上面获取到的BeanClass是否带有Aspect注解
						if (this.advisorFactory.isAspect(beanType)) {
                            //添加到 aspectNames中
							aspectNames.add(beanName);
                            //创建切面元数据
							AspectMetadata amd = new AspectMetadata(beanType, beanName);
							if (amd.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
                                //注意这里放入了 BeanFactory的引用 方便后面从BeanFactory中获取切面的实例
								MetadataAwareAspectInstanceFactory factory =
										new BeanFactoryAspectInstanceFactory(this.beanFactory, beanName);
								List<Advisor> classAdvisors = this.advisorFactory.getAdvisors(factory);
								if (this.beanFactory.isSingleton(beanName)) {
                                    //如果是单例的，缓存起来
									this.advisorsCache.put(beanName, classAdvisors);
								}
								else {
									this.aspectFactoryCache.put(beanName, factory);
								}
								advisors.addAll(classAdvisors);
							}
							else {
								// Per target or per this.
								if (this.beanFactory.isSingleton(beanName)) {
									throw new IllegalArgumentException("Bean with name '" + beanName +
											"' is a singleton, but aspect instantiation model is not singleton");
								}
                                //这里使用的是 PrototypeAspectInstanceFactory
								MetadataAwareAspectInstanceFactory factory =
										new PrototypeAspectInstanceFactory(this.beanFactory, beanName);
								this.aspectFactoryCache.put(beanName, factory);
								advisors.addAll(this.advisorFactory.getAdvisors(factory));
							}
						}
					}
					this.aspectBeanNames = aspectNames;
					return advisors;
				}
			}
		}

		if (aspectNames.isEmpty()) {
			return Collections.emptyList();
		}
		List<Advisor> advisors = new ArrayList<>();
		for (String aspectName : aspectNames) {
			List<Advisor> cachedAdvisors = this.advisorsCache.get(aspectName);
			if (cachedAdvisors != null) {
				advisors.addAll(cachedAdvisors);
			}
			else {
				MetadataAwareAspectInstanceFactory factory = this.aspectFactoryCache.get(aspectName);
				advisors.addAll(this.advisorFactory.getAdvisors(factory));
			}
		}
		return advisors;
	}

	/**
	 * Return whether the aspect bean with the given name is eligible.
	 * @param beanName the name of the aspect bean
	 * @return whether the bean is eligible
	 */
	protected boolean isEligibleBean(String beanName) {
		return true;
	}

}

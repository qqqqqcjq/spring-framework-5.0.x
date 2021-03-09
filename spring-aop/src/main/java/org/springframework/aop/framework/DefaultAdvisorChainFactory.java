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

package org.springframework.aop.framework;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.aop.Advisor;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.support.MethodMatchers;
import org.springframework.lang.Nullable;

/**
 * A simple but definitive way of working out an advice chain for a Method,
 * given an {@link Advised} object. Always rebuilds each advice chain;
 * caching can be provided by subclasses.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Adrian Colyer
 * @since 2.0.3
 */
@SuppressWarnings("serial")
public class DefaultAdvisorChainFactory implements AdvisorChainFactory, Serializable {

	@Override
    //获取拦截器链
    //在这个方法中传入了三个实例，一个是Advised的实例 一个是目标方法 一个是目标类可能为null
    /**
     * 在上面这个方法中主要干了这几件事：
     * 1、循环目标方法的所有Advisor
     * 2、判断Advisor的类型
     * 如果是PointcutAdvisor的类型，则判断此Advisor是否适用于此目标方法
     * 如果是IntroductionAdvisor引介增强类型，则判断此Advisor是否适用于此目标类
     * 如果以上都不是，则直接转换为Interceptor类型，添加到拦截器链
     */
	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(
			Advised config, Method method, @Nullable Class<?> targetClass) {

		// This is somewhat tricky... We have to process introductions first,
		// but we need to preserve order in the ultimate list.

        //创建一个初始大小为 之前获取到的 通知个数的 集合
		List<Object> interceptorList = new ArrayList<Object>(config.getAdvisors().length);

		//如果目标类为null的话，则从方法签名中获取目标类
		Class<?> actualClass = (targetClass != null ? targetClass : method.getDeclaringClass());

		//判断目标类是否存在引介增强 通常为false
		boolean hasIntroductions = hasMatchingIntroductions(config, actualClass);

		// 这里用了一个单例模式 获取DefaultAdvisorAdapterRegistry实例
        // 在Spring中把每一个功能都分的很细，每个功能都会有相应的类去处理 符合单一职责原则的地方很多 这也是值得我们借鉴的一个地方
        // DefaultAdvisorAdapterRegistry里面注册了一组AdvisorAdapter，
        // AdvisorAdapterRegistry这个类的主要作用是将Advice适配为Advisor 将Advisor适配为对应的MethodInterceptor 我们在下面说明
		AdvisorAdapterRegistry registry = GlobalAdvisorAdapterRegistry.getInstance();

		//循环所有Advisor，获取拦截器链
		for (Advisor advisor : config.getAdvisors()) {

            //如果是PointcutAdvisor类型的实例  我们大多数的Advisor都是PointcutAdvisor类型的
			if (advisor instanceof PointcutAdvisor) {
				// Add it conditionally.
				PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;

				if (config.isPreFiltered() || pointcutAdvisor.getPointcut().getClassFilter().matches(actualClass)) {

                    //Advisor中可以获取PointCut,PointCut可以获取MethodMatcher，使用MethodMatcher判断方法是否满足当前PointCut
				    MethodMatcher mm = pointcutAdvisor.getPointcut().getMethodMatcher();

					//检测Advisor中Pointcut的是否适用于此目标方法
					if (MethodMatchers.matches(mm, method, actualClass, hasIntroductions)) {

                        //从Advisor获取拦截器链的重要方法
						MethodInterceptor[] interceptors = registry.getInterceptors(advisor);

                        //MethodMatcher中的切点分为两种 一个是静态的 一种是动态的
                        //如果isRuntime返回true 则是动态的切入点 ，则使用当前的MethodInterceptor创建一个InterceptorAndDynamicMethodMatcher加入拦截器链
                        //每次方法的调用都要去进行匹配；
                        //而静态切入点则会将MethodInterceptor直接加入拦截器链。
						if (mm.isRuntime()) {
							// Creating a new object instance in the getInterceptors() method
							// isn't a problem as we normally cache created chains.
							for (MethodInterceptor interceptor : interceptors) {
								interceptorList.add(new InterceptorAndDynamicMethodMatcher(interceptor, mm));
							}
						}
						else {
							interceptorList.addAll(Arrays.asList(interceptors));
						}
					}
				}
			}

            //如果是引介增强
			else if (advisor instanceof IntroductionAdvisor) {
				IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
				if (config.isPreFiltered() || ia.getClassFilter().matches(actualClass)) {
                    //从Advisor获取拦截器链的重要方法
					Interceptor[] interceptors = registry.getInterceptors(advisor);
					interceptorList.addAll(Arrays.asList(interceptors));
				}
			}
            //以上两种都不是
			else {
                //从Advisor获取拦截器链的重要方法
                //将Advisor转换为Interceptor类型。这里用到了一个很重要的一个类：DefaultAdvisorAdapterRegistry。从类名我们可以看出这是一个Advisor的适配器注册类。
				Interceptor[] interceptors = registry.getInterceptors(advisor);

				interceptorList.addAll(Arrays.asList(interceptors));
			}
		}

		return interceptorList;
	}

	/**
	 * Determine whether the Advisors contain matching introductions.
	 */
	private static boolean hasMatchingIntroductions(Advised config, Class<?> actualClass) {
		for (Advisor advisor : config.getAdvisors()) {
			if (advisor instanceof IntroductionAdvisor) {
				IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
				if (ia.getClassFilter().matches(actualClass)) {
					return true;
				}
			}
		}
		return false;
	}

}

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

package org.springframework.aop.framework.adapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;

/**
 * Default implementation of the {@link AdvisorAdapterRegistry} interface.
 * Supports {@link org.aopalliance.intercept.MethodInterceptor},
 * {@link org.springframework.aop.MethodBeforeAdvice},
 * {@link org.springframework.aop.AfterReturningAdvice},
 * {@link org.springframework.aop.ThrowsAdvice}.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class DefaultAdvisorAdapterRegistry implements AdvisorAdapterRegistry, Serializable {

    //初始化了一个AdvisorAdapter的集合
    private final List<AdvisorAdapter> adapters = new ArrayList<>(3);

	/**
	 * Create a new DefaultAdvisorAdapterRegistry, registering well-known adapters.
	 */
	public DefaultAdvisorAdapterRegistry() {

        //在SpringAOP中只默认提供了这三种通知类型的适配器
        //为什么没有其他通知类型的呢？

        /**
         * AdvisorAdapter是顶层接口，允许对Spring AOP框架进行扩展，从而允许处理新的顾问和通知类型
         * 目前只有下面3个Advice, 因为下面3个Advice只实现了Advice接口, 没有实现org.aopalliance.intercept.MethodInterceptor接口
         * 所以，DefaultAdvisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice方法获取拦截器链的时候，会使用AdvisorAdapter判断是不是下面这些Advice,
         * 是的话AdvisorAdapter会使用这个Adviced调用AdvisorAdapter.getInterceptor()创建对应的MethodInterceptor
         * AfterReturningAdvice ： AfterReturningAdviceAdapter ==>AfterReturningAdviceInterceptor
         * MethodBeforeAdvice ： MethodBeforeAdviceAdapter ==>MethodBeforeAdviceInterceptor
         * ThrowsAdvice ： ThrowsAdviceAdapter ==>ThrowsAdviceInterceptor
         */

        /**
         * 即实现了Advice 又实现了MethodInterceptor的可以直接加入拦截器链
         * AspectJAroundAdvice
         * AspectJMethodBeforeAdvice
         * AspectJAfterAdvice
         * AspectJAfterReturningAdvice
         * AspectJAfterThrowingAdvice
         */
		registerAdvisorAdapter(new MethodBeforeAdviceAdapter());
		registerAdvisorAdapter(new AfterReturningAdviceAdapter());
		registerAdvisorAdapter(new ThrowsAdviceAdapter());
	}


	@Override
    //这个方法的作用主要是将Advice转换为Advisor的
	public Advisor wrap(Object adviceObject) throws UnknownAdviceTypeException {
        //如果传入的实例是Advisor 则直接返回
		if (adviceObject instanceof Advisor) {
			return (Advisor) adviceObject;
		}
        //如果传入的实例不是 Advice类型 则直接抛出异常
		if (!(adviceObject instanceof Advice)) {
			throw new UnknownAdviceTypeException(adviceObject);
		}
		Advice advice = (Advice) adviceObject;

        //如果这个Advice是MethodInterceptor类型的实例，则直接包装为DefaultPointcutAdvisor
        //DefaultPointcutAdvisor中的Pointcut为Pointcut.TRUE matches始终返回true
		if (advice instanceof MethodInterceptor) {
			// So well-known it doesn't even need an adapter.
			return new DefaultPointcutAdvisor(advice);
		}

        //如果不是Advisor的实例 也不是MethodInterceptor类型的实例
        //看看是不是 上面的那种通知类型适配器所支持的类型
		for (AdvisorAdapter adapter : this.adapters) {
			// Check that it is supported.
			if (adapter.supportsAdvice(advice)) {
				return new DefaultPointcutAdvisor(advice);
			}
		}
		throw new UnknownAdviceTypeException(advice);
	}

	@Override
    //获取拦截器链的重要方法
	public MethodInterceptor[] getInterceptors(Advisor advisor) throws UnknownAdviceTypeException {
		List<MethodInterceptor> interceptors = new ArrayList<>(3);

        /**
         * AdvisorAdapter是顶层接口，允许对Spring AOP框架进行扩展，从而允许处理新的顾问和通知类型
         * 目前只有下面3个Advice, 因为下面3个Advice只实现了Advice接口, 没有实现org.aopalliance.intercept.MethodInterceptor接口
         * 所以，DefaultAdvisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice方法获取拦截器链的时候，会使用AdvisorAdapter判断是不是下面这些Advice,
         * 是的话AdvisorAdapter会使用这个Adviced调用AdvisorAdapter.getInterceptor()创建对应的MethodInterceptor
         * AfterReturningAdvice ： AfterReturningAdviceAdapter ==>AfterReturningAdviceInterceptor
         * MethodBeforeAdvice ： MethodBeforeAdviceAdapter ==>MethodBeforeAdviceInterceptor
         * ThrowsAdvice ： ThrowsAdviceAdapter ==>ThrowsAdviceInterceptor
         */

        /**
         * 即实现了Advice 又实现了MethodInterceptor的可以直接加入拦截器链
         * AspectJAroundAdvice
         * AspectJMethodBeforeAdvice
         * AspectJAfterAdvice
         * AspectJAfterReturningAdvice
         * AspectJAfterThrowingAdvice
         */
        Advice advice = advisor.getAdvice();

		//即实现了Advice 又实现了MethodInterceptor的可以直接加入拦截器链
		if (advice instanceof MethodInterceptor) {
			interceptors.add((MethodInterceptor) advice);
		}

		//只实现了Advice,没有实现MethodInterceptor，则使用对应的AdvisorAdapter创建对应的MethodInterceptor
		for (AdvisorAdapter adapter : this.adapters) {
			if (adapter.supportsAdvice(advice)) {
				interceptors.add(adapter.getInterceptor(advisor));
			}
		}

		if (interceptors.isEmpty()) {
			throw new UnknownAdviceTypeException(advisor.getAdvice());
		}
		return interceptors.toArray(new MethodInterceptor[0]);
	}

	@Override
	public void registerAdvisorAdapter(AdvisorAdapter adapter) {
		this.adapters.add(adapter);
	}

}

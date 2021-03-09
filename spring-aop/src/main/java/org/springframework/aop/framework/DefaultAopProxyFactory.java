/*
 * Copyright 2002-2015 the original author or authors.
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
import java.lang.reflect.Proxy;

import org.springframework.aop.SpringProxy;

/**
 * Default {@link AopProxyFactory} implementation, creating either a CGLIB proxy
 * or a JDK dynamic proxy.
 *
 * <p>Creates a CGLIB proxy if one the following is true for a given
 * {@link AdvisedSupport} instance:
 * <ul>
 * <li>the {@code optimize} flag is set
 * <li>the {@code proxyTargetClass} flag is set
 * <li>no proxy interfaces have been specified
 * </ul>
 *
 * <p>In general, specify {@code proxyTargetClass} to enforce a CGLIB proxy,
 * or specify one or more interfaces to use a JDK dynamic proxy.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 12.03.2004
 * @see AdvisedSupport#setOptimize
 * @see AdvisedSupport#setProxyTargetClass
 * @see AdvisedSupport#setInterfaces
 */

// 在SpringAOP中提供了两种创建代理对象的方式，一种是JDK自带的方式创建代理对象，另一种是使用Cglib的方式创建代理对象。
// 所以在SpringAOP中抽象了一个AopProxy接口，这个接口有两个实现类：JDKDynamicAopProxy和CglibAopProxy。从名字我们应该能看出来这两个类的作用了吧。
// 在为目标类创建代理对象的时候，根据我们的目标类型和AOP的配置信息选择不同的创建代理对象的方式。在SpringAOP中创建代理对象没有直接依赖AopProxy，
// 而是又抽象了一个AopProxyFactory的接口，通过这个接口（工厂模式）来创建代理对象。
// 在SpringAOP中 AopProxyFactory只有一个实现类，这个实现类就是DefaultAopProxyFactory
@SuppressWarnings("serial")
public class DefaultAopProxyFactory implements AopProxyFactory, Serializable {

	@Override
    // 这里传入了一个参数 AdvisedSupport
    // 这段代码用来判断选择哪种创建代理对象的方式
	public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
		//config.isOptimize()  是否对代理类的生成使用策略优化 其作用是和isProxyTargetClass是一样的，默认是false,可以在xml中配置，注解的配置暂时没找到
		//config.isProxyTargetClass()是否使用Cglib的方式创建代理对象 默认为false，默认是false,可以在注解中配置，EnableAspectJAutoProxy注解中的proxyTargetClass
        //hasNoUserSuppliedProxyInterfaces ：目标类没有接口为true, 有且只有一个SpringProxy接口为true, 其余为false
        if (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)) {

            //从AdvisedSupport中获取目标类 类对象
            Class<?> targetClass = config.getTargetClass();
			if (targetClass == null) {
				throw new AopConfigException("TargetSource cannot determine target class: " +
						"Either an interface or a target is required for proxy creation.");
			}
            //判断目标类是否是接口 如果目标类是接口的话，则还是使用JDK的方式生成代理对象
            //如果目标类是Proxy类型 则还是使用JDK的方式生成代理对象
			if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
				return new JdkDynamicAopProxy(config);
			}
			//使用cglib创建代理对象
			return new ObjenesisCglibAopProxy(config);
		}
		else {
			return new JdkDynamicAopProxy(config);
		}
	}

	/**
	 * Determine whether the supplied {@link AdvisedSupport} has only the
	 * {@link org.springframework.aop.SpringProxy} interface specified
	 * (or no proxy interfaces specified at all).
	 */
	private boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
		Class<?>[] ifcs = config.getProxiedInterfaces();
		return (ifcs.length == 0 || (ifcs.length == 1 && SpringProxy.class.isAssignableFrom(ifcs[0])));
	}

}

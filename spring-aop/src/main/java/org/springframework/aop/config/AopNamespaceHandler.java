/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.aop.config;

import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * {@code NamespaceHandler} for the {@code aop} namespace.
 *
 * <p>Provides a {@link org.springframework.beans.factory.xml.BeanDefinitionParser} for the
 * {@code <aop:config>} tag. A {@code config} tag can include nested
 * {@code pointcut}, {@code advisor} and {@code aspect} tags.
 *
 * <p>The {@code pointcut} tag allows for creation of named
 * {@link AspectJExpressionPointcut} beans using a simple syntax:
 * <pre class="code">
 * &lt;aop:pointcut id=&quot;getNameCalls&quot; expression=&quot;execution(* *..ITestBean.getName(..))&quot;/&gt;
 * </pre>
 *
 * <p>Using the {@code advisor} tag you can configure an {@link org.springframework.aop.Advisor}
 * and have it applied to all relevant beans in you {@link org.springframework.beans.factory.BeanFactory}
 * automatically. The {@code advisor} tag supports both in-line and referenced
 * {@link org.springframework.aop.Pointcut Pointcuts}:
 *
 * <pre class="code">
 * &lt;aop:advisor id=&quot;getAgeAdvisor&quot;
 *     pointcut=&quot;execution(* *..ITestBean.getAge(..))&quot;
 *     advice-ref=&quot;getAgeCounter&quot;/&gt;
 *
 * &lt;aop:advisor id=&quot;getNameAdvisor&quot;
 *     pointcut-ref=&quot;getNameCalls&quot;
 *     advice-ref=&quot;getNameCounter&quot;/&gt;</pre>
 *
 * @author Rob Harrop
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @since 2.0
 */


//我们在使用Spring中不同的功能的时候可能会引入不同的命名空间比如xmlns:context，xmlns:aop，xmlns:tx等等。
//在Spring中定义了一个这样的抽象类专门用来解析不同的命名空间。这个类是NamespaceHandler。
//AopNamespaceHandler就继承自这个类

//Spring套路
//xml配置的方式 ：每个框架对应的NamespaceHandler就是你分析Spring中的对应框架的关键入口。
//注解方式 ： 一般会使用一个@EnableXXX 引入需要的组件，注解通过ImportBeanDefinitionRegistrar技术引入需要的组件。

//注册bean是Sprng的主流程
//Xml方式是使用XmlBeanDefinitionReader->DefaultBeanDefinitionDocumentReader ，不需要继承NamespaceHandler。(NamespaceHandler一般解析的是一个特定命名空间下的标签，一个特定的命名空间往往是一个专门引入的功能，比如aop命名空间)
//注解方式是使用AnnotatedBeanDefinitionReader
public class AopNamespaceHandler extends NamespaceHandlerSupport {

	/**
	 * Register the {@link BeanDefinitionParser BeanDefinitionParsers} for the
	 * '{@code config}', '{@code spring-configured}', '{@code aspectj-autoproxy}'
	 * and '{@code scoped-proxy}' tags.
	 */
	@Override
	public void init() {
		// In 2.0 XSD as well as in 2.1 XSD.
		registerBeanDefinitionParser("config", new ConfigBeanDefinitionParser());

        //我们看到了这样的一段代码  aspectj-autoproxy这个再加上aop 是不是就是 aop:aspectj-autoproxy呢
        //这段代码的意思是使用AspectJAutoProxyBeanDefinitionParser来解析<aop:aspectj-autoproxy>标签
        //AspectJAutoProxyBeanDefinitionParser这个类就是SpringAOP和Spring框架结合的关键
        //遇到这个标签时，就会使用来解析，他会给Spring 注册AnnotationAwareAspectJAutoProxyCreator这个bean，这个就是Spring的一个扩展点，
        //父类有SmartInstantiationAwareBeanPostProcessor，Spring ioc流程中就会触发创建代理的流程。
        //当然，除了使用<aop:aspectj-autoproxy>标签,我们也可以使用@EnableAspectJAutoProxy注解，这个注解通过ImportBeanDefinitionRegistrar技术引入AnnotationAwareAspectJAutoProxyCreator这个bean
		registerBeanDefinitionParser("aspectj-autoproxy", new AspectJAutoProxyBeanDefinitionParser());

		registerBeanDefinitionDecorator("scoped-proxy", new ScopedProxyBeanDefinitionDecorator());

		// Only in 2.0 XSD: moved to context namespace as of 2.1
		registerBeanDefinitionParser("spring-configured", new SpringConfiguredBeanDefinitionParser());
	}

}

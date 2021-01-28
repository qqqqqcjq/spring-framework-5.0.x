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

package org.springframework.web.method.annotation;

import javax.servlet.ServletException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Resolves method arguments annotated with {@code @Value}.
 *
 * <p>An {@code @Value} does not have a name but gets resolved from the default
 * value string, which may contain ${...} placeholder or Spring Expression
 * Language #{...} expressions.
 *
 * <p>A {@link WebDataBinder} may be invoked to apply type conversion to
 * resolved argument value.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
//  它用于处理标注有@Value注解的参数。对于这个注解我们太熟悉不过了，没想到在web层依旧能发挥作用。
//  通过@Value让我们在配置文件里给参数赋值，在某些特殊场合（比如前端不用传，但你想给个默认值，这个时候用它也是一种方案）
//  说明：这就相当于在Controller层使用了@Value注解，其实我是不太建议的。因为@Value建议还是只使用在业务层

//  原理就是自己不提供resolveArgument方法，使用父类AbstractNamedValueMethodArgumentResolver.resolveArgument#resolveStringValue(String value)得到StandardBeanExpressionResolver这个工具类，计算#{...}，得到参数值
//
//  在MVC子容器中导入外部化配置
//  @Configuration
//  @PropertySource("classpath:my.properties") // 此处有键值对：test.myage = 18
//  @EnableWebMvc
//  public class WebMvcConfig extends WebMvcConfigurerAdapter { ... }

//  controller层方法
//  @ResponseBody
//  @GetMapping("/test")
//  public Object test(@Value("#{T(Integer).parseInt('${test.myage:10}') + 10}") int myAge) {
//      System.out.println(myAge);
//      return myAge;
//  }
//
//  请求：/test，打印：28。
//  注意：若你写成@Value("#{'${test.myage:10}' + 10}，那你得到的答案是：1810（成字符串拼接了）。
//
//  另外，我看到网上有不少人说如果把这个@PropertySource("classpath:my.properties")放在根容器的config文件里导入，controller层就使用@Value/占位符获取不到值了，其实这是**不正确**的。理由如下：
//  Spring MVC子容器在创建时：initWebApplicationContext()
//
//  if (cwac.getParent() == null) {
//  	cwac.setParent(rootContext); // 设置上父容器（根容器）
//  }
//
//  AbstractApplicationContext：如下代码
//  	// 相当于子容器的环境会把父容器的Enviroment合并进来
//  	@Override
//  	public void setParent(@Nullable ApplicationContext parent) {
//  		this.parent = parent;
//  		if (parent != null) {
//  			Environment parentEnvironment = parent.getEnvironment();
//  			if (parentEnvironment instanceof ConfigurableEnvironment) {
//  				getEnvironment().merge((ConfigurableEnvironment) parentEnvironment);
//  			}
//  		}
//  	}
//
//  AbstractEnvironment：merge()方法如下
//  	@Override
//  	public void merge(ConfigurableEnvironment parent) {
//  		// 完全的从parent里所有的PropertySources里拷贝一份进来
//  		for (PropertySource<?> ps : parent.getPropertySources()) {
//  			if (!this.propertySources.contains(ps.getName())) {
//  				this.propertySources.addLast(ps);
//  			}
//  		}
//  		...
//  	}
//  这就是为什么说即使你是在根容器里使用的@PropertySource导入的外部资源，子容器也可以使用的原因（因为子容器会把父环境给merge一份过来）。
//  但是：如果你是使用形如PropertyPlaceholderConfigurer这种方式导进来的，那是会有容器隔离效应的，PropertyPlaceholderConfigurer的引入方法参见PropertyPlaceholderConfigurer类的注释
public class ExpressionValueMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	/**
	 * @param beanFactory a bean factory to use for resolving  ${...}
	 * placeholder and #{...} SpEL expressions in default values;
	 * or {@code null} if default values are not expected to contain expressions
	 */
    // 唯一构造函数  支持占位符、SpEL，将容器作为参数传进来
	public ExpressionValueMethodArgumentResolver(@Nullable ConfigurableBeanFactory beanFactory) {
		super(beanFactory);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(Value.class);
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		Value ann = parameter.getParameterAnnotation(Value.class);
		Assert.state(ann != null, "No Value annotation");
		return new ExpressionValueNamedValueInfo(ann);
	}

	@Override
	@Nullable
	protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest webRequest) throws Exception {
		// No name to resolve
		return null;
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletException {
		throw new UnsupportedOperationException("@Value is never required: " + parameter.getMethod());
	}


	private static class ExpressionValueNamedValueInfo extends NamedValueInfo {

		private ExpressionValueNamedValueInfo(Value annotation) {
			super("@Value", false, annotation.value());
		}
	}

}

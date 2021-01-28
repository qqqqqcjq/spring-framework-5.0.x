/*
 * Copyright 2002-2013 the original author or authors.
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

/**
 * Enumerates the various scoped-proxy options.
 *
 * <p>For a more complete discussion of exactly what a scoped proxy is, see the
 * section of the Spring reference documentation entitled '<em>Scoped beans as
 * dependencies</em>'.
 *
 * @author Mark Fisher
 * @since 2.5
 * @see ScopeMetadata
 */
// 指明是否为检测到的组件生成代理
// <!--web 环境中 ioc 容器测试scoped-proxy  =============begin ==================-->
// <!-- 将一个HTTP Session bean 暴露为一个代理bean -->
// <bean id="userPreferences" class="com.luban.springmvc.UserPreferences" scope="session">
//     <!-- 通知Spring容器去代理这个bean -->
//     <aop:scoped-proxy/>
// </bean>
// <!-- 将上述bean 的代理注入到一个单例bean -->
// <bean id="userService" class="com.luban.springmvc.SimpleUserService" scope="singleton">
//     <!-- 引用被代理的 userPreferences bean -->
//     <property name="userPreferences" ref="userPreferences"/>
// </bean>
// <!--web 环境中 ioc 容器测试scoped-proxy  =============end ==================-->
//
// 如果删除<aop:scoped-proxy/>这个标签的话就会报错
// [ERROR] Context initialization failed
// org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'userService' defined in class path resource [applicationContext.xml]: Cannot resolve reference to bean 'userPreferences' while setting bean property 'userPreferences'; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'userPreferences': Scope 'session' is not active for the current thread; consider defining a scoped proxy for this bean if you intend to refer to it from a singleton; nested exception is java.lang.IllegalStateException: No thread-bound request found: Are you referring to request attributes outside of an actual web request, or processing a request outside of the originally receiving thread? If you are actually operating within a web request and still receive this message, your code is probably running outside of DispatcherServlet/DispatcherPortlet: In this case, use RequestContextListener or RequestContextFilter to expose the current request.
// 报错的意思为：创建在类路径资源[applicationContext]中定义名为'userService'的bean时出错。在设置bean属性'userPreferences'时不能解析对bean 'userPreferences'的引用;
// aop:scoped-proxy的作用如下：
// 当scope session还没有创建时，我们必须做特殊处理，比如加<aop:scoped-proxy>来修饰短生命周期的bean。为什么？
// 其实也好理解。比如测试用例中的生命周期长的bean 的类型是Singleton，还没有用户访问时，在最初的时刻就建立了，而且只建立一次。
// 这时它的一个属性却要急着指向另外一个session类型的bean ，而session类型的bean的生命周期短（只有当有用户访问时，才会创建session, 有了session 才会创建bean）。
// 现在处于初始阶段，还没有用户上网呢，不可能创建session, 当然也就不能创建bean。所以<aop:scoped-proxy>的意思, 如果bean 的作用域是session, 那么容器初始化时，其他bean需要注入这个bean, 那么就用一个代理对象注入。（该代理对象拥有和userPreferences完全相同的public接口。调用代理对象方法时，代理对象会从Session范围内获取真正的userPreferences对象，调用其方法）。
// 如果去除<aop:scoped-proxy /> 会报上面的错误

// 上面是xml中对单个bean的配置， 可以在@ComponentScan中指明一个默认的值
public enum ScopedProxyMode {

	/**
	 * Default typically equals {@link #NO}, unless a different default has been configured at the component-scan instruction level.
	 */
	DEFAULT,

	/**
	 * Do not create a scoped proxy.
	 * <p>This proxy-mode is not typically useful when used with a
	 * non-singleton scoped instance, which should favor the use of the
	 * {@link #INTERFACES} or {@link #TARGET_CLASS} proxy-modes instead if it
	 * is to be used as a dependency.
	 */
	NO,

	/**
	 * Create a JDK dynamic proxy implementing <i>all</i> interfaces exposed by the class of the target object.
	 */
	INTERFACES,

	/**
	 * Create a class-based proxy (uses CGLIB).
	 */
	TARGET_CLASS;

}

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

package org.springframework.context.annotation;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * A single {@code condition} that must be {@linkplain #matches matched} in order
 * for a component to be registered.
 *
 * <p>Conditions are checked immediately before the bean-definition is due to be
 * registered and are free to veto registration based on any criteria that can
 * be determined at that point.
 *
 * <p>Conditions must follow the same restrictions as {@link BeanFactoryPostProcessor}
 * and take care to never interact with bean instances. For more fine-grained control
 * of conditions that interact with {@code @Configuration} beans consider the
 * {@link ConfigurationCondition} interface.
 *
 * @author Phillip Webb
 * @since 4.0
 * @see ConfigurationCondition
 * @see Conditional
 * @see ConditionContext
 */
@FunctionalInterface
//Spring Framework中使用的地方：
//1 . 扫描的时候获取@Condition注解信息，调用ConditionEvaluator.shouldSkip(AnnotatedTypeMetadata metadata)，实例化该类的@Condition注解引入的Condition实现类， 执行每一个matches看看是否满足所有条件，满足的话可以扫描注册
//2 . 将bean class作为配置类进行配置类的处理时，调用ConditionEvaluator.shouldSkip(,)，实例化该类的@Condition注解引入的Condition实现类， 执行每一个matches看看是否满足所有条件，满足的话可以扫描注册
//3 . 扫描注册@Bean metnod，调用ConditionEvaluator.shouldSkip(,)，实例化该@Bean method引入的@Condition注解引入的Condition实现类， 执行每一个matches看看是否满足所有条件，满足的话可以扫描注册
// ========================================= 以@Profile注解为例  begin =========================================
// @Conditional(ProfileCondition.class)
// public @interface Profile {
// 	String[] value();
// }
// @Profile注解上有@Condition注解，引入的ProfileCondition，我们使用@Profile注解注解类或者@Bean method的时候，指定一个value,
// ConditionEvaluator.shouldSkip调用matches方法，比较ConditionContextImpl.environment中的profile值和@Profile直接的value是否一致，
// 一致的话就扫描注册这个bean, 不一致的话就跳过

// Environment里面封装这很多上下文信息
// ========================================= 以@Profile注解为例  end   =========================================
public interface Condition {

	/**
	 * Determine if the condition matches.
	 * @param context the condition context
	 * @param metadata metadata of the {@link org.springframework.core.type.AnnotationMetadata class}
	 * or {@link org.springframework.core.type.MethodMetadata method} being checked
	 * @return {@code true} if the condition matches and the component can be registered,
	 * or {@code false} to veto the annotated component's registration
	 */
	boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata);

}

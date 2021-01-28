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

package org.springframework.beans.factory.config;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;

import kotlin.reflect.KProperty;
import kotlin.reflect.jvm.ReflectJvmMapping;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

/**
 * 工具类DependencyDescriptor描述一个用于注入的依赖。这样一个对象用于包装一个成员属性依赖，一个构造函数参数依赖或者成员方法参数依赖。
 * 此描述器工具对将要注入的依赖进行包装之后，对这些信息的访问方式就统一了。
 *
 * 因为继承自InjectionPoint,一个依赖描述符DependencyDescriptor首先包含了自己所描述的注入点的信息，除此之外，它还包含了其他一些信息 :
 *
 * 依赖是否必要required;
 * 是否饥饿加载eager;
 * 嵌套级别 nestingLevel;
 * 以及其它一些工具方法。
 *
 * 一个Spring管理的bean, 一般有多个依赖描述，也就是有多个DependencyDescriptor实例，
 * 有的是属性依赖描述，有的是参数依赖描述
 *
 * Descriptor for a specific dependency that is about to be injected.
 * Wraps a constructor parameter, a method parameter or a field,
 * allowing unified access to their metadata.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
@SuppressWarnings("serial")
public class DependencyDescriptor extends InjectionPoint implements Serializable {

    // 保存所包装依赖(成员属性或者成员方法的某个参数)所在的声明类，
    // 其实该信息在 field/methodParameter 中已经隐含
	private final Class<?> declaringClass;

	@Nullable
    // 如果所包装依赖是成员方法的某个参数，则这里记录该成员方法的名称
	private String methodName;

	@Nullable
    // 如果所包装的是成员方法的某个参数，则这里记录该参数的类型
	private Class<?>[] parameterTypes;

    // 如果所包装的是成员方法的某个参数，则这里记录该参数在该函数参数列表中的索引
	private int parameterIndex;

	@Nullable
    //如果所包装的是成员属性，则这里记录该成员属性的名称
	private String fieldName;

    // 标识所包装依赖是否必要依赖
	private final boolean required;

    // 标识所包装依赖是否需要饥饿加载
	private final boolean eager;

    // 标识所包装依赖的嵌套级别
	private int nestingLevel = 1;

	@Nullable
    //一个特定的包含类(可能是已声明类的子类，例如替换一个类型变量),通常和声明类是同一个
	private Class<?> containingClass;

	@Nullable
    // 所包装依赖 ResolvableType 的缓存
	private volatile ResolvableType resolvableType;

	//这个版本已经没有这个属性了，老版本有，这里加上注释一下
    // 所包装依赖 TypeDescriptor 的缓存
    @Nullable
    private transient volatile TypeDescriptor typeDescriptor;

	/**
     * 构造函数，包装成员方法参数依赖，依赖解析会使用 饥饿模式
	 * Create a new descriptor for a method or constructor parameter.
	 * Considers the dependency as 'eager'.
	 * @param methodParameter the MethodParameter to wrap
	 * @param required whether the dependency is required
	 */
	public DependencyDescriptor(MethodParameter methodParameter, boolean required) {
		this(methodParameter, required, true);
	}

	/**
     * 构造函数，包装成员方法参数依赖
	 * Create a new descriptor for a method or constructor parameter.
	 * @param methodParameter the MethodParameter to wrap
	 * @param required whether the dependency is required
	 * @param eager whether this dependency is 'eager' in the sense of
	 * eagerly resolving potential target beans for type matching
	 */
	public DependencyDescriptor(MethodParameter methodParameter, boolean required, boolean eager) {
		super(methodParameter);

		this.declaringClass = methodParameter.getDeclaringClass();
		if (methodParameter.getMethod() != null) {
			this.methodName = methodParameter.getMethod().getName();
		}
		this.parameterTypes = methodParameter.getExecutable().getParameterTypes();
		this.parameterIndex = methodParameter.getParameterIndex();
		this.containingClass = methodParameter.getContainingClass();
		this.required = required;
		this.eager = eager;
	}

	/**
     * 构造函数，包装成员属性依赖，依赖解析会使用 饥饿模式
	 * Create a new descriptor for a field.
	 * Considers the dependency as 'eager'.
	 * @param field the field to wrap
	 * @param required whether the dependency is required
	 */
	public DependencyDescriptor(Field field, boolean required) {
		this(field, required, true);
	}

	/**
     * 构造函数，包装成员属性依赖
	 * Create a new descriptor for a field.
	 * @param field the field to wrap
	 * @param required whether the dependency is required
	 * @param eager whether this dependency is 'eager' in the sense of
	 * eagerly resolving potential target beans for type matching
	 */
	public DependencyDescriptor(Field field, boolean required, boolean eager) {
		super(field);

		this.declaringClass = field.getDeclaringClass();
		this.fieldName = field.getName();
		this.required = required;
		this.eager = eager;
	}

	/**
     * 复制构造函数
	 * Copy constructor.
	 * @param original the original descriptor to create a copy from
	 */
	public DependencyDescriptor(DependencyDescriptor original) {
		super(original);

		this.declaringClass = original.declaringClass;
		this.methodName = original.methodName;
		this.parameterTypes = original.parameterTypes;
		this.parameterIndex = original.parameterIndex;
		this.fieldName = original.fieldName;
		this.containingClass = original.containingClass;
		this.required = original.required;
		this.eager = original.eager;
		this.nestingLevel = original.nestingLevel;
	}


	/**
     * 返回此依赖项是否必要
	 * Return whether this dependency is required.
	 * <p>Optional semantics are derived from Java 8's {@link java.util.Optional},
	 * any variant of a parameter-level {@code Nullable} annotation (such as from
	 * JSR-305 or the FindBugs set of annotations), or a language-level nullable
	 * type declaration in Kotlin.
	 */
	public boolean isRequired() {
		if (!this.required) {
			return false;
		}

		if (this.field != null) {
			return !(this.field.getType() == Optional.class || hasNullableAnnotation() ||
					(KotlinDetector.isKotlinType(this.field.getDeclaringClass()) &&
							KotlinDelegate.isNullable(this.field)));
		}
		else {
			return !obtainMethodParameter().isOptional();
		}
	}

	/**
	 * Check whether the underlying field is annotated with any variant of a
	 * {@code Nullable} annotation, e.g. {@code javax.annotation.Nullable} or
	 * {@code edu.umd.cs.findbugs.annotations.Nullable}.
	 */
	private boolean hasNullableAnnotation() {
		for (Annotation ann : getAnnotations()) {
			if ("Nullable".equals(ann.annotationType().getSimpleName())) {
				return true;
			}
		}
		return false;
	}

	/**
     * 依赖解析时是否使用饥饿模式
	 * Return whether this dependency is 'eager' in the sense of
	 * eagerly resolving potential target beans for type matching.
	 */
	public boolean isEager() {
		return this.eager;
	}

	/**
	 * Resolve the specified not-unique scenario: by default,
	 * throwing a {@link NoUniqueBeanDefinitionException}.
	 * <p>Subclasses may override this to select one of the instances or
	 * to opt out with no result at all through returning {@code null}.
	 * @param type the requested bean type
	 * @param matchingBeans a map of bean names and corresponding bean
	 * instances which have been pre-selected for the given type
	 * (qualifiers etc already applied)
	 * @return a bean instance to proceed with, or {@code null} for none
	 * @throws BeansException in case of the not-unique scenario being fatal
	 * @since 4.3
	 */
	@Nullable
	public Object resolveNotUnique(Class<?> type, Map<String, Object> matchingBeans) throws BeansException {
		throw new NoUniqueBeanDefinitionException(type, matchingBeans.keySet());
	}

	/**
	 * Resolve a shortcut for this dependency against the given factory, for example
	 * taking some pre-resolved information into account.
	 * <p>The resolution algorithm will first attempt to resolve a shortcut through this
	 * method before going into the regular type matching algorithm across all beans.
	 * Subclasses may override this method to improve resolution performance based on
	 * pre-cached information while still receiving {@link InjectionPoint} exposure etc.
	 * @param beanFactory the associated factory
	 * @return the shortcut result if any, or {@code null} if none
	 * @throws BeansException if the shortcut could not be obtained
	 * @since 4.3.1
	 */
	@Nullable
	public Object resolveShortcut(BeanFactory beanFactory) throws BeansException {
		return null;
	}

	/**
     * 使用指定的容器 beanFactory 获取所包装依赖对应的 bean 实例
     * 该方法会被容器用于自动注入依赖前获取该依赖所对应的 bean 实例
     * 这个功能很重要
	 * Resolve the specified bean name, as a candidate result of the matching
	 * algorithm for this dependency, to a bean instance from the given factory.
	 * <p>The default implementation calls {@link BeanFactory#getBean(String)}.
	 * Subclasses may provide additional arguments or other customizations.
	 * @param beanName the bean name, as a candidate result for this dependency
	 * @param requiredType the expected type of the bean (as an assertion)
	 * @param beanFactory the associated factory
	 * @return the bean instance (never {@code null})
	 * @throws BeansException if the bean could not be obtained
	 * @since 4.3.2
	 * @see BeanFactory#getBean(String)
	 */
	public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory)
			throws BeansException {

		return beanFactory.getBean(beanName);
	}


	/**
	 * Increase this descriptor's nesting level.
	 * @see MethodParameter#increaseNestingLevel()
	 */
	public void increaseNestingLevel() {
		this.nestingLevel++;
		this.resolvableType = null;
		if (this.methodParameter != null) {
			this.methodParameter.increaseNestingLevel();
		}
	}

	/**
	 * Optionally set the concrete class that contains this dependency.
	 * This may differ from the class that declares the parameter/field in that
	 * it may be a subclass thereof, potentially substituting type variables.
	 * @since 4.0
	 */
	public void setContainingClass(Class<?> containingClass) {
		this.containingClass = containingClass;
		this.resolvableType = null;
		if (this.methodParameter != null) {
			GenericTypeResolver.resolveParameterType(this.methodParameter, containingClass);
		}
	}

	/**
	 * Build a ResolvableType object for the wrapped parameter/field.
	 * @since 4.0
	 */
	public ResolvableType getResolvableType() {
		ResolvableType resolvableType = this.resolvableType;
		if (resolvableType == null) {
			resolvableType = (this.field != null ?
					ResolvableType.forField(this.field, this.nestingLevel, this.containingClass) :
					ResolvableType.forMethodParameter(obtainMethodParameter()));
			this.resolvableType = resolvableType;
		}
		return resolvableType;
	}

	/**
	 * Return whether a fallback match is allowed.
	 * <p>This is {@code false} by default but may be overridden to return {@code true} in order
	 * to suggest to an {@link org.springframework.beans.factory.support.AutowireCandidateResolver}
	 * that a fallback match is acceptable as well.
	 * @since 4.0
	 */
	public boolean fallbackMatchAllowed() {
		return false;
	}

	/**
	 * Return a variant of this descriptor that is intended for a fallback match.
	 * @since 4.0
	 * @see #fallbackMatchAllowed()
	 */
	public DependencyDescriptor forFallbackMatch() {
		return new DependencyDescriptor(this) {
			@Override
			public boolean fallbackMatchAllowed() {
				return true;
			}
		};
	}

	/**
	 * Initialize parameter name discovery for the underlying method parameter, if any.
	 * <p>This method does not actually try to retrieve the parameter name at
	 * this point; it just allows discovery to happen when the application calls
	 * {@link #getDependencyName()} (if ever).
	 */
	public void initParameterNameDiscovery(@Nullable ParameterNameDiscoverer parameterNameDiscoverer) {
		if (this.methodParameter != null) {
			this.methodParameter.initParameterNameDiscovery(parameterNameDiscoverer);
		}
	}

	/**
	 * Determine the name of the wrapped parameter/field.
	 * @return the declared name (never {@code null})
	 */
	@Nullable
	public String getDependencyName() {
		return (this.field != null ? this.field.getName() : obtainMethodParameter().getParameterName());
	}

	/**
	 * Determine the declared (non-generic) type of the wrapped parameter/field.
	 * @return the declared type (never {@code null})
	 */
	public Class<?> getDependencyType() {
		if (this.field != null) {
			if (this.nestingLevel > 1) {
				Type type = this.field.getGenericType();
				for (int i = 2; i <= this.nestingLevel; i++) {
					if (type instanceof ParameterizedType) {
						Type[] args = ((ParameterizedType) type).getActualTypeArguments();
						type = args[args.length - 1];
					}
				}
				if (type instanceof Class) {
					return (Class<?>) type;
				}
				else if (type instanceof ParameterizedType) {
					Type arg = ((ParameterizedType) type).getRawType();
					if (arg instanceof Class) {
						return (Class<?>) arg;
					}
				}
				return Object.class;
			}
			else {
				return this.field.getType();
			}
		}
		else {
			return obtainMethodParameter().getNestedParameterType();
		}
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!super.equals(other)) {
			return false;
		}
		DependencyDescriptor otherDesc = (DependencyDescriptor) other;
		return (this.required == otherDesc.required && this.eager == otherDesc.eager &&
				this.nestingLevel == otherDesc.nestingLevel && this.containingClass == otherDesc.containingClass);
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Rely on default serialization; just initialize state after deserialization.
		ois.defaultReadObject();

		// Restore reflective handles (which are unfortunately not serializable)
		try {
			if (this.fieldName != null) {
				this.field = this.declaringClass.getDeclaredField(this.fieldName);
			}
			else {
				if (this.methodName != null) {
					this.methodParameter = new MethodParameter(
							this.declaringClass.getDeclaredMethod(this.methodName, this.parameterTypes), this.parameterIndex);
				}
				else {
					this.methodParameter = new MethodParameter(
							this.declaringClass.getDeclaredConstructor(this.parameterTypes), this.parameterIndex);
				}
				for (int i = 1; i < this.nestingLevel; i++) {
					this.methodParameter.increaseNestingLevel();
				}
			}
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Could not find original class structure", ex);
		}
	}


	/**
	 * Inner class to avoid a hard dependency on Kotlin at runtime.
	 */
	private static class KotlinDelegate {

		/**
		 * Check whether the specified {@link Field} represents a nullable Kotlin type or not.
		 */
		public static boolean isNullable(Field field) {
			KProperty<?> property = ReflectJvmMapping.getKotlinProperty(field);
			return (property != null && property.getReturnType().isMarkedNullable());
		}
	}

}

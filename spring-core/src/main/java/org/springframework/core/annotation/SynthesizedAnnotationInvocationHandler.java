/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * 给一个注解使用动态代理
 * {@link InvocationHandler} for an {@link Annotation} that Spring has
 * <em>synthesized</em> (i.e., wrapped in a dynamic proxy) with additional
 * functionality.
 *
 * @author Sam Brannen
 * @since 4.2
 * @see Annotation
 * @see AnnotationAttributeExtractor
 * @see AnnotationUtils#synthesizeAnnotation(Annotation, AnnotatedElement)
 */
class SynthesizedAnnotationInvocationHandler implements InvocationHandler {

	private final AnnotationAttributeExtractor<?> attributeExtractor;

	private final Map<String, Object> valueCache = new ConcurrentHashMap<>(8);


	/**
	 * Construct a new {@code SynthesizedAnnotationInvocationHandler} for
	 * the supplied {@link AnnotationAttributeExtractor}.
	 * @param attributeExtractor the extractor to delegate to
	 */
	SynthesizedAnnotationInvocationHandler(AnnotationAttributeExtractor<?> attributeExtractor) {
		Assert.notNull(attributeExtractor, "AnnotationAttributeExtractor must not be null");
		this.attributeExtractor = attributeExtractor;
	}


	@Override
    // 在invoke（即拦截方法中，这个拦截方法就是在注解中获取属性值的方法，不要忘了，注解的属性实际上定义为接口的方法），
    // 其次判断，如果当前执行的方法不是equals、hashCodeWebMvcConfigurer、toString、或者属性是另外的注解，或者不是属性方法，之外的方法（这些方法就是要处理的目标属性），
    // 都调用了getAttributeValue方法，所以我们又跟踪到getAttributeValue方法的重要代码：
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (ReflectionUtils.isEqualsMethod(method)) {
			return annotationEquals(args[0]);
		}
		if (ReflectionUtils.isHashCodeMethod(method)) {
			return annotationHashCode();
		}
		if (ReflectionUtils.isToStringMethod(method)) {
			return annotationToString();
		}
		if (AnnotationUtils.isAnnotationTypeMethod(method)) {
			return annotationType();
		}
		if (!AnnotationUtils.isAttributeMethod(method)) {
			throw new AnnotationConfigurationException(String.format(
					"Method [%s] is unsupported for synthesized annotation type [%s]", method, annotationType()));
		}
		return getAttributeValue(method);
	}

	private Class<? extends Annotation> annotationType() {
		return this.attributeExtractor.getAnnotationType();
	}

	//所有的注解默认继承Annotation接口， 注解也是一个类
	//Annotation接口定义的方法是普通方法，注解中定义的方法叫属性方法，也就是它既是一个属性(使用注解时直接给它赋值)，也是一个方法(可以使用这个属性方法获取值)
	private Object getAttributeValue(Method attributeMethod) {
		String attributeName = attributeMethod.getName();
		//先尝试从缓存中获取注解的属性值
		Object value = this.valueCache.get(attributeName);
		if (value == null) {

		    // attributeExtractor是一个AnnotationAttributeExtractor类型，这个对象是在构造SynthesizedAnnotationInvocationHandler时传入的，默认是一个DefaultAnnotationAttributeExtractor对象；
            // 而DefaultAnnotationAttributeExtractor是继承AbstractAliasAwareAnnotationAttributeExtractor，看名字，真正的处理AliasFor标签的动作，应该就在这里面，于是继续看代码：
			value = this.attributeExtractor.getAttributeValue(attributeMethod);
			if (value == null) {
				String msg = String.format("%s returned null for attribute name [%s] from attribute source [%s]",
						this.attributeExtractor.getClass().getName(), attributeName, this.attributeExtractor.getSource());
				throw new IllegalStateException(msg);
			}

			// Synthesize nested annotations before returning them.
			if (value instanceof Annotation) {
				value = AnnotationUtils.synthesizeAnnotation((Annotation) value, this.attributeExtractor.getAnnotatedElement());
			}
			else if (value instanceof Annotation[]) {
				value = AnnotationUtils.synthesizeAnnotationArray((Annotation[]) value, this.attributeExtractor.getAnnotatedElement());
			}

			this.valueCache.put(attributeName, value);
		}

		// Clone arrays so that users cannot alter the contents of values in our cache.
		if (value.getClass().isArray()) {
			value = cloneArray(value);
		}

		return value;
	}

	/**
	 * Clone the provided array, ensuring that original component type is
	 * retained.
	 * @param array the array to clone
	 */
	private Object cloneArray(Object array) {
		if (array instanceof boolean[]) {
			return ((boolean[]) array).clone();
		}
		if (array instanceof byte[]) {
			return ((byte[]) array).clone();
		}
		if (array instanceof char[]) {
			return ((char[]) array).clone();
		}
		if (array instanceof double[]) {
			return ((double[]) array).clone();
		}
		if (array instanceof float[]) {
			return ((float[]) array).clone();
		}
		if (array instanceof int[]) {
			return ((int[]) array).clone();
		}
		if (array instanceof long[]) {
			return ((long[]) array).clone();
		}
		if (array instanceof short[]) {
			return ((short[]) array).clone();
		}

		// else
		return ((Object[]) array).clone();
	}

	/**
	 * See {@link Annotation#equals(Object)} for a definition of the required algorithm.
	 * @param other the other object to compare against
	 */
	private boolean annotationEquals(Object other) {
		if (this == other) {
			return true;
		}
		if (!annotationType().isInstance(other)) {
			return false;
		}

		for (Method attributeMethod : AnnotationUtils.getAttributeMethods(annotationType())) {
			Object thisValue = getAttributeValue(attributeMethod);
			Object otherValue = ReflectionUtils.invokeMethod(attributeMethod, other);
			if (!ObjectUtils.nullSafeEquals(thisValue, otherValue)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * See {@link Annotation#hashCode()} for a definition of the required algorithm.
	 */
	private int annotationHashCode() {
		int result = 0;

		for (Method attributeMethod : AnnotationUtils.getAttributeMethods(annotationType())) {
			Object value = getAttributeValue(attributeMethod);
			int hashCode;
			if (value.getClass().isArray()) {
				hashCode = hashCodeForArray(value);
			}
			else {
				hashCode = value.hashCode();
			}
			result += (127 * attributeMethod.getName().hashCode()) ^ hashCode;
		}

		return result;
	}

	/**
	 * WARNING: we can NOT use any of the {@code nullSafeHashCode()} methods
	 * in Spring's {@link ObjectUtils} because those hash code generation
	 * algorithms do not comply with the requirements specified in
	 * {@link Annotation#hashCode()}.
	 * @param array the array to compute the hash code for
	 */
	private int hashCodeForArray(Object array) {
		if (array instanceof boolean[]) {
			return Arrays.hashCode((boolean[]) array);
		}
		if (array instanceof byte[]) {
			return Arrays.hashCode((byte[]) array);
		}
		if (array instanceof char[]) {
			return Arrays.hashCode((char[]) array);
		}
		if (array instanceof double[]) {
			return Arrays.hashCode((double[]) array);
		}
		if (array instanceof float[]) {
			return Arrays.hashCode((float[]) array);
		}
		if (array instanceof int[]) {
			return Arrays.hashCode((int[]) array);
		}
		if (array instanceof long[]) {
			return Arrays.hashCode((long[]) array);
		}
		if (array instanceof short[]) {
			return Arrays.hashCode((short[]) array);
		}

		// else
		return Arrays.hashCode((Object[]) array);
	}

	/**
	 * See {@link Annotation#toString()} for guidelines on the recommended format.
	 */
	private String annotationToString() {
		StringBuilder sb = new StringBuilder("@").append(annotationType().getName()).append("(");

		Iterator<Method> iterator = AnnotationUtils.getAttributeMethods(annotationType()).iterator();
		while (iterator.hasNext()) {
			Method attributeMethod = iterator.next();
			sb.append(attributeMethod.getName());
			sb.append('=');
			sb.append(attributeValueToString(getAttributeValue(attributeMethod)));
			sb.append(iterator.hasNext() ? ", " : "");
		}

		return sb.append(")").toString();
	}

	private String attributeValueToString(Object value) {
		if (value instanceof Object[]) {
			return "[" + StringUtils.arrayToDelimitedString((Object[]) value, ", ") + "]";
		}
		return String.valueOf(value);
	}

}

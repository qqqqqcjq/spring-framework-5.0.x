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

package org.springframework.core.type;

import java.util.Set;

/**
 * Interface that defines abstract access to the annotations of a specific class, in a form that does not require that class to be loaded yet.
 *
 * 接口，定义了对特定类的注解的访问，AnnotationMetadataReadingVisitor实现类基于ASM,所以访问该类的注解的时候直接访问的是磁盘上的字节码文件，不需要将类加载到JVM中
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 2.5
 * @see StandardAnnotationMetadata
 * @see org.springframework.core.type.classreading.MetadataReader#getAnnotationMetadata()
 * @see AnnotatedTypeMetadata
 */
//AnnotationMetadata : 这是理解Spring注解编程的必备知识，它是ClassMetadata和AnnotatedTypeMetadata的子接口，具有两者共同能力，并且新增了访问注解的相关方法。可以简单理解为它是对注解的抽象。
//有基于反射和ASM两种实现
//StandardAnnotationMetadata主要使用 Java 反射原理获取元数据，标准反射：它依赖于Class，优点是实现简单，缺点是使用时必须把Class加载进来。
//而 AnnotationMetadataReadingVisitor 使用 ASM 框架获取元数据。ASM：无需提前加载Class入JVM，所有特别特别适用于形如Spring应用扫描的场景（扫描所有资源，但并不是加载所有进JVM/容器）
public interface AnnotationMetadata extends ClassMetadata, AnnotatedTypeMetadata {

	/**
	 * Get the fully qualified class names of all annotation types that
	 * are <em>present</em> on the underlying class.
	 * @return the annotation type names
	 */
    //拿到当前类上所有的注解的全类名（注意是全类名）
	Set<String> getAnnotationTypes();

	/**
	 * Get the fully qualified class names of all meta-annotation types that
	 * are <em>present</em> on the given annotation type on the underlying class.
	 * @param annotationName the fully qualified class name of the meta-annotation
	 * type to look for
	 * @return the meta-annotation type names, or an empty set if none found
	 */
    // 拿到指定的注解类型
    //annotationName:注解类型的全类名
	Set<String> getMetaAnnotationTypes(String annotationName);

	/**
	 * Determine whether an annotation of the given type is <em>present</em> on
	 * the underlying class.
	 * @param annotationName the fully qualified class name of the annotation
	 * type to look for
	 * @return {@code true} if a matching annotation is present
	 */
    // 是否包含指定注解 （annotationName：全类名）
	boolean hasAnnotation(String annotationName);

	/**
	 * Determine whether the underlying class has an annotation that is itself
	 * annotated with the meta-annotation of the given type.
	 * @param metaAnnotationName the fully qualified class name of the
	 * meta-annotation type to look for
	 * @return {@code true} if a matching meta-annotation is present
	 */
    //这个厉害了，用于判断注解类型自己是否被某个元注解类型所标注
    //依赖于AnnotatedElementUtils#hasMetaAnnotationTypes
	boolean hasMetaAnnotation(String metaAnnotationName);

	/**
	 * Determine whether the underlying class has any methods that are
	 * annotated (or meta-annotated) with the given annotation type.
	 * @param annotationName the fully qualified class name of the annotation
	 * type to look for
	 */
    // 类里面只要有一个方法标注有指定注解，就返回true
    //getDeclaredMethods获得所有方法， AnnotatedElementUtils.isAnnotated是否标注有指定注解
	boolean hasAnnotatedMethods(String annotationName);

	/**
	 * Retrieve the method metadata for all methods that are annotated
	 * (or meta-annotated) with the given annotation type.
	 * <p>For any returned method, {@link MethodMetadata#isAnnotated} will
	 * return {@code true} for the given annotation type.
	 * @param annotationName the fully qualified class name of the annotation
	 * type to look for
	 * @return a set of {@link MethodMetadata} for methods that have a matching
	 * annotation. The return value will be an empty set if no methods match
	 * the annotation type.
	 */
    // 返回所有的标注有指定注解的方法元信息。注意返回的是MethodMetadata 原理基本同上
	Set<MethodMetadata> getAnnotatedMethods(String annotationName);

}

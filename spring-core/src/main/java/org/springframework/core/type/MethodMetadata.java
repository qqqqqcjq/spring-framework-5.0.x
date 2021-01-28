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

package org.springframework.core.type;

/**
 * Interface that defines abstract access to the annotations of a specific
 * class, in a form that does not require that class to be loaded yet.
 *
 * @author Juergen Hoeller
 * @author Mark Pollack
 * @author Chris Beams
 * @author Phillip Webb
 * @since 3.0
 * @see StandardMethodMetadata
 * @see AnnotationMetadata#getAnnotatedMethods
 * @see AnnotatedTypeMetadata
 */
//Method的元数据，继承了AnnotatedTypeMetadata，所以除了获取一般方法的元数据，还可以获取带注解的方法上的注解信息
//有基于反射和ASM两种实现
//StandardMethodMetadata主要使用 Java 反射原理获取元数据，标准反射：它依赖于Class，优点是实现简单，缺点是使用时必须把Class加载进来。
//而 MethodMetadataReadingVisitor使用 ASM 框架获取元数据。ASM：无需提前加载Class入JVM，所有特别特别适用于形如Spring应用扫描的场景（扫描所有资源，但并不是加载所有进JVM/容器）
public interface MethodMetadata extends AnnotatedTypeMetadata {

	/**
	 * Return the name of the method.
	 */
	//返回该方法的名字
	String getMethodName();

	/**
	 * Return the fully-qualified name of the class that declares this method.
	 */
	//返回声明此方法的类的全限定名。
	String getDeclaringClassName();

	/**
	 * Return the fully-qualified name of this method's declared return type.
	 * @since 4.2
	 */
	//返回该方法返回值类型的名字
	String getReturnTypeName();

	/**
	 * Return whether the underlying method is effectively abstract:
	 * i.e. marked as abstract on a class or declared as a regular,
	 * non-default method in an interface.
	 * @since 4.2
	 */
	//是否是抽象方法
	boolean isAbstract();

	/**
	 * Return whether the underlying method is declared as 'static'.
	 */
	//是否是静态方法
	boolean isStatic();

	/**
	 * Return whether the underlying method is marked as 'final'.
	 */
	//是否是final方法
	boolean isFinal();

	/**
	 * Return whether the underlying method is overridable,
	 * i.e. not marked as static, final or private.
	 */
	//是否是重写的方法
	boolean isOverridable();

}

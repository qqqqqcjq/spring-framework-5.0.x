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

import org.springframework.lang.Nullable;

/**
 * Interface that defines abstract metadata of a specific class,
 * in a form that does not require that class to be loaded yet.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see StandardClassMetadata
 * @see org.springframework.core.type.classreading.MetadataReader#getClassMetadata()
 * @see AnnotationMetadata
 */
//顶层接口
//对Class的抽象和适配, 此接口的所有方法，基本上都跟Class有关
//有2种实现：
//StandardClassMetadata 基于反射
//ClassMetadataReadingVisitor  基于ASM
public interface ClassMetadata {

	/**
	 * Return the name of the underlying class.
	 */
	// 返回类名（注意返回的是最原始的那个className）
	String getClassName();

	/**
	 * Return whether the underlying class represents an interface.
	 */
    //是否是接口
	boolean isInterface();

	/**
	 * Return whether the underlying class represents an annotation.
	 * @since 4.1
	 */
    // 是否是注解
	boolean isAnnotation();

	/**
	 * Return whether the underlying class is marked as abstract.
	 */
    // 是否是抽象类
	boolean isAbstract();

	/**
	 * Return whether the underlying class represents a concrete class,
	 * i.e. neither an interface nor an abstract class.
	 */
    // 是否允许创建  不是接口且不是抽象类  这里就返回true了
	boolean isConcrete();

	/**
	 * Return whether the underlying class is marked as 'final'.
	 */
	//是否是final类
	boolean isFinal();

	/**
	 * Determine whether the underlying class is independent, i.e. whether
	 * it is a top-level class or a nested class (static inner class) that
	 * can be constructed independently from an enclosing class.
	 */
    // 是否是独立的(能够创建对象的)  比如是Class、或者内部类、静态内部类
	boolean isIndependent();

	/**
	 * Return whether the underlying class is declared within an enclosing
	 * class (i.e. the underlying class is an inner/nested class or a
	 * local class within a method).
	 * <p>If this method returns {@code false}, then the underlying
	 * class is a top-level class.
	 */
    // 是否有内部类
	boolean hasEnclosingClass();

	/**
	 * Return the name of the enclosing class of the underlying class,
	 * or {@code null} if the underlying class is a top-level class.
	 */
	@Nullable
    //获取当前类的直接外部类Class对象
	String getEnclosingClassName();

	/**
	 * Return whether the underlying class has a super class.
	 */
	//是否有父类
	boolean hasSuperClass();

	/**
	 * Return the name of the super class of the underlying class,
	 * or {@code null} if there is no super class defined.
	 */
	@Nullable
    //获取父类的class name
	String getSuperClassName();

	/**
	 * Return the names of all interfaces that the underlying class
	 * implements, or an empty array if there are none.
	 */
	//获取所有接口的名字
	String[] getInterfaceNames();

	/**
	 * Return the names of all classes declared as members of the class represented by
	 * this ClassMetadata object. This includes public, protected, default (package)
	 * access, and private classes and interfaces declared by the class, but excludes
	 * inherited classes and interfaces. An empty array is returned if no member classes
	 * or interfaces exist.
	 * @since 3.1
	 */
    // 获取当前类的内部类
	String[] getMemberClassNames();

}
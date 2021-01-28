/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.core.type.classreading;

import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;

/**
 * Simple facade for accessing class metadata,
 * as read by an ASM {@link org.springframework.asm.ClassReader}.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
//此接口是一个访问ClassMetadata等的简单门面，实现是委托给org.springframework.asm.ClassReader、ClassVisitor来处理的，
//它不用把Class加载进JVM就可以拿到元数据，因为它读取的是资源：Resource，这是它最大的优势所在。
public interface MetadataReader {

	/**
	 * Return the resource reference for the class file.
	 */
    // 返回此Class文件的来源（资源）
	Resource getResource();

	/**
	 * Read basic class metadata for the underlying class.
	 */
    // 返回此Class的元数据信息
	ClassMetadata getClassMetadata();

	/**
	 * Read full annotation metadata for the underlying class,
	 * including metadata for annotated methods.
	 */
    // 返回此类的注解元信息（包括方法的）
	AnnotationMetadata getAnnotationMetadata();

}

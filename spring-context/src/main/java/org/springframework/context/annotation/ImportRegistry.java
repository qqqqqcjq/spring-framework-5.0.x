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

package org.springframework.context.annotation;

import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;

/**
 * @author Juergen Hoeller
 * @author Phil Webb
 */
interface ImportRegistry {

	@Nullable
    //从ConfigurationClassParser.ImportStack.imports得到importedClass映射的注解元数据
	AnnotationMetadata getImportingClassFor(String importedClass);

	//从ConfigurationClassParser.ImportStack.imports删除importedClass和他映射的注解元数据
	void removeImportingClass(String importingClass);

}

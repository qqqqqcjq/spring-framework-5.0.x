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

package org.springframework.core.annotation;

/**
 * Marker interface implemented by synthesized annotation proxies.
 *
 * <p>Used to detect whether an annotation has already been synthesized.
 *
 * @author Sam Brannen
 * @since 4.2
 */
//SynthesizedAnnotation是一个标记接口，给注解生成代理时会让代理实现这个接口，标记是一个注解的代理
public interface SynthesizedAnnotation {
}

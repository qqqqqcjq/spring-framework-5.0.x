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
 * Enumeration of the type filters that may be used in conjunction with
 * {@link ComponentScan @ComponentScan}.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2.5
 * @see ComponentScan
 * @see ComponentScan#includeFilters()
 * @see ComponentScan#excludeFilters()
 * @see org.springframework.core.type.filter.TypeFilter
 */
public enum FilterType {

	/**
	 * Filter candidates marked with a given annotation.
	 * @see org.springframework.core.type.filter.AnnotationTypeFilter
	 */
	//使用MetadataReader中的信息，判断是否有给定的注解
	ANNOTATION,

	/**
	 * Filter candidates assignable to a given type.
	 * @see org.springframework.core.type.filter.AssignableTypeFilter
	 */
    //使用MetadataReader中的信息，判断是否是指定的类型
	ASSIGNABLE_TYPE,

	/**
	 * Filter candidates matching a given AspectJ type pattern expression.
	 * @see org.springframework.core.type.filter.AspectJTypeFilter
	 */
	//aspectj 使用  MetadataReader 得到特定信息，然后再使用AspectJTypeFilter判断这个特定信息是否满足条件
	ASPECTJ,

	/**
	 * Filter candidates matching a given regex pattern.
	 * @see org.springframework.core.type.filter.RegexPatternTypeFilter
	 */
	//使用MetadataReader中的信息，判断类名是否满足给定的正则表达式
    //使用的是java.util下面的正则表达式匹配工具 ：
    // 1. Pattern.compile(expression)  使用给定的正则表达式构造一个 Pattern对象
    // 2. pattern.matcher(metadata.getClassName()) 使用 pattern和类名构造一个Matcher对象
    // 3. matcher.matches() 方法判断类名是否满足给定的正则表达式
	REGEX,

	/** Filter candidates using a given custom
	 * {@link org.springframework.core.type.filter.TypeFilter} implementation.
	 */
	//基于MetadataReader中的信息， 自己实现一个TypeFilter进行筛选
    //spring boot中就定义了2个自己的TypeFilter   TypeExcludeFilter  AutoConfigurationExcludeFilter
    //@ComponentScan(excludeFilters = { @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
    //		@Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class) })
	CUSTOM

}

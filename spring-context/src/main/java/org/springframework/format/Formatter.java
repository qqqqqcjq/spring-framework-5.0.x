/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.format;

/**
 * Formats objects of type T.
 * A Formatter is both a Printer <i>and</i> a Parser for an object type.
 *
 * @author Keith Donald
 * @since 3.0
 * @param <T> the type of object this Formatter formats
 */
//Spring 3.0还新增了一个Formatter<T>接口，作用为：将Object格式化为类型T。从语义上理解它也具有类型转换（数据转换的作用），相较于Converter<S,T>它强调的是「格式化」，因此一般用于时间/日期、数字（小数、分数、科学计数法等等）、货币等场景
//为了和类型转换服务ConversionService完成整合，对外只提供统一的API。Spring提供了FormattingConversionService专门用于整合Converter和Formatter，从而使得两者具有一致的编程体验，对开发者更加友好。
public interface Formatter<T> extends Printer<T>, Parser<T> {

}

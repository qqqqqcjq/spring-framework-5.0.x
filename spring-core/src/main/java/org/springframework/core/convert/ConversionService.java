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

package org.springframework.core.convert;

import org.springframework.lang.Nullable;

/**
 * A service interface for type conversion. This is the entry point into the convert system.
 * Call {@link #convert(Object, Class)} to perform a thread-safe type conversion using this system.
 *
 * @author Keith Donald
 * @author Phillip Webb
 * @since 3.0
 */
//Converter、ConverterFactory、GenericConverter它们三都着力于完成类型转换。对于使用者而言，如果做个类型转换需要了解到这三套体系无疑成本太高，因此就有了ConversionService用于整合它们三，统一化接口操作。
//此接口也是Spring 3.0新增，用于「统一化」 底层类型转换实现的差异，对外提供统一服务，所以它也被称作类型转换的「门面接口」，从接口名称xxxService也能看出来其设计思路。它主要有两大实现：
//GenericConversionService：提供模版实现，如转换器的注册、删除、匹配查找等，但并不内置转换器实现
//DefaultConversionService：继承自GenericConversionService。在它基础上默认注册了非常多的内建的转换器实现，从而能够实现「绝大部分」的类型转换需求

// 用于类型转换的服务接口,这是进入转换系统的入口点。
// 调用{@link #convert(Object, Class)}来执行类型转换

//整个转换体系围绕着 Converter   GenericConverter ConverterFactory， 这3个转换器参考石墨文档
public interface ConversionService {

	/**
	 * Return {@code true} if objects of {@code sourceType} can be converted to the {@code targetType}.
	 * <p>If this method returns {@code true}, it means {@link #convert(Object, Class)} is capable
	 * of converting an instance of {@code sourceType} to {@code targetType}.
	 * <p>Special note on collections, arrays, and maps types:
	 * For conversion between collection, array, and map types, this method will return {@code true}
	 * even though a convert invocation may still generate a {@link ConversionException} if the
	 * underlying elements are not convertible. Callers are expected to handle this exceptional case
	 * when working with collections and maps.
	 * @param sourceType the source type to convert from (may be {@code null} if source is {@code null})
	 * @param targetType the target type to convert to (required)
	 * @return {@code true} if a conversion can be performed, {@code false} if not
	 * @throws IllegalArgumentException if {@code targetType} is {@code null}
	 */
	//只在子类GenericConversionService有一个实现，判断当前类是否可以转换成目标类
	boolean canConvert(@Nullable Class<?> sourceType, Class<?> targetType);

	/**
	 * Return {@code true} if objects of {@code sourceType} can be converted to the {@code targetType}.
	 * The TypeDescriptors provide additional context about the source and target locations
	 * where conversion would occur, often object fields or property locations.
	 * <p>If this method returns {@code true}, it means {@link #convert(Object, TypeDescriptor, TypeDescriptor)}
	 * is capable of converting an instance of {@code sourceType} to {@code targetType}.
	 * <p>Special note on collections, arrays, and maps types:
	 * For conversion between collection, array, and map types, this method will return {@code true}
	 * even though a convert invocation may still generate a {@link ConversionException} if the
	 * underlying elements are not convertible. Callers are expected to handle this exceptional case
	 * when working with collections and maps.
	 * @param sourceType context about the source type to convert from
	 * (may be {@code null} if source is {@code null})
	 * @param targetType context about the target type to convert to (required)
	 * @return {@code true} if a conversion can be performed between the source and target types,
	 * {@code false} if not
	 * @throws IllegalArgumentException if {@code targetType} is {@code null}
	 */
    //只在子类GenericConversionService有一个实现，判断当前类是否可以转换成目标类
	boolean canConvert(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType);

	/**
	 * Convert the given {@code source} to the specified {@code targetType}.
	 * @param source the source object to convert (may be {@code null})
	 * @param targetType the target type to convert to (required)
	 * @return the converted object, an instance of targetType
	 * @throws ConversionException if a conversion exception occurred
	 * @throws IllegalArgumentException if targetType is {@code null}
	 */
	@Nullable
    //只在子类GenericConversionService有一个实现，将当前类转换为目标类， 使用GenericConverter完成类型转换
    //ConverterAdapter是Convert和GenericConverter的适配器
    //ConverterFactory里面是Convert的工厂
	<T> T convert(@Nullable Object source, Class<T> targetType);

	/**
	 * Convert the given {@code source} to the specified {@code targetType}.
	 * The TypeDescriptors provide additional context about the source and target locations
	 * where conversion will occur, often object fields or property locations.
	 * @param source the source object to convert (may be {@code null})
	 * @param sourceType context about the source type to convert from
	 * (may be {@code null} if source is {@code null})
	 * @param targetType context about the target type to convert to (required)
	 * @return the converted object, an instance of {@link TypeDescriptor#getObjectType() targetType}
	 * @throws ConversionException if a conversion exception occurred
	 * @throws IllegalArgumentException if targetType is {@code null},
	 * or {@code sourceType} is {@code null} but source is not {@code null}
	 */
	@Nullable
    //只在子类GenericConversionService有一个实现，将当前类转换为目标类， 使用GenericConverter完成类型转换
    //ConverterAdapter是Convert和GenericConverter的适配器
    //ConverterFactory里面是Convert的工厂
	Object convert(@Nullable Object source, @Nullable TypeDescriptor sourceType, TypeDescriptor targetType);

}

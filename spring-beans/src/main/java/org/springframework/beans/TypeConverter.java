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

package org.springframework.beans;

import java.lang.reflect.Field;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;


/**
 * Interface that defines type conversion methods. Typically (but not necessarily)
 * implemented in conjunction with the {@link PropertyEditorRegistry} interface.
 *
 * <p><b>Note:</b> Since TypeConverter implementations are typically based on
 * {@link java.beans.PropertyEditor PropertyEditors} which aren't thread-safe,
 * TypeConverters themselves are <em>not</em> to be considered as thread-safe either.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see SimpleTypeConverter
 * @see BeanWrapperImpl
 */
// !!!定义类型转换方法的接口，它在Spring 2.0就已经存在。在还没有ConversionService之前，它的类型转换动作均委托给已注册的PropertyEditor来完成。!!!
// !!!但自3.0之后，这个转换动作可能被PropertyEditor来做，也可能交给ConversionService处理。!!!

//定义类型转换方法的接口,通常(但不一定)与{@link PropertyEditorRegistry}接口一起实现。
//因为TypeConverter实现通常基于PropertyEditor
//PropertyEditor接口只针对一个类型，TypeConverter可以处理多种类型，TypeConverter的实现里面封装了多个PropertyEditor和Converversionservice
public interface TypeConverter {

    /**
     * Convert the value to the required type (if necessary from a String).
     * <p>Conversions from String to any type will typically use the {@code setAsText}
     * method of the PropertyEditor class, or a Spring Converter in a ConversionService.
     * @param value the value to convert
     * @param requiredType the type we must convert to
     * (or {@code null} if not known, for example in case of a collection element)
     * @return the new value, possibly the result of type conversion
     * @throws TypeMismatchException if type conversion failed
     * @see java.beans.PropertyEditor#setAsText(String)
     * @see java.beans.PropertyEditor#getValue()
     * @see org.springframework.core.convert.ConversionService
     * @see org.springframework.core.convert.converter.Converter
     */
    @Nullable
    //将值转换为所需的类型(如果需要，从String类型)。String到任何类型的转换通常使用PropertyEditor类中的{@code setAsText}方法，或者Converversionservice
    //这个方法里面兼容了PropertyEditor和Converversionservice
    <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType) throws TypeMismatchException;

    /**
     * Convert the value to the required type (if necessary from a String).
     * <p>Conversions from String to any type will typically use the {@code setAsText}
     * method of the PropertyEditor class, or a Spring Converter in a ConversionService.
     * @param value the value to convert
     * @param requiredType the type we must convert to
     * (or {@code null} if not known, for example in case of a collection element)
     * @param methodParam the method parameter that is the target of the conversion
     * (for analysis of generic types; may be {@code null})
     * @return the new value, possibly the result of type conversion
     * @throws TypeMismatchException if type conversion failed
     * @see java.beans.PropertyEditor#setAsText(String)
     * @see java.beans.PropertyEditor#getValue()
     * @see org.springframework.core.convert.ConversionService
     * @see org.springframework.core.convert.converter.Converter
     */
    @Nullable
    //将值转换为所需的类型(如果需要，从String类型)。String到任何类型的转换通常使用PropertyEditor类中的{@code setAsText}方法，或者Converversionservice
    //这个方法里面兼容了PropertyEditor和Converversionservice
    <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
                             @Nullable MethodParameter methodParam) throws TypeMismatchException;

    /**
     * Convert the value to the required type (if necessary from a String).
     * <p>Conversions from String to any type will typically use the {@code setAsText}
     * method of the PropertyEditor class, or a Spring Converter in a ConversionService.
     * @param value the value to convert
     * @param requiredType the type we must convert to
     * (or {@code null} if not known, for example in case of a collection element)
     * @param field the reflective field that is the target of the conversion
     * (for analysis of generic types; may be {@code null})
     * @return the new value, possibly the result of type conversion
     * @throws TypeMismatchException if type conversion failed
     * @see java.beans.PropertyEditor#setAsText(String)
     * @see java.beans.PropertyEditor#getValue()
     * @see org.springframework.core.convert.ConversionService
     * @see org.springframework.core.convert.converter.Converter
     */
    @Nullable
    //将值转换为所需的类型(如果需要，从String类型)。String到任何类型的转换通常使用PropertyEditor类中的{@code setAsText}方法，或者Converversionservice
    //这个方法里面兼容了PropertyEditor和Converversionservice
    <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable Field field)
            throws TypeMismatchException;

}

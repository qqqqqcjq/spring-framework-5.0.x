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

package org.springframework.beans;

import java.beans.PropertyEditor;

import org.springframework.lang.Nullable;

/**
 * Encapsulates methods for registering JavaBeans {@link PropertyEditor PropertyEditors}.
 * This is the central interface that a {@link PropertyEditorRegistrar} operates on.
 *
 * <p>Extended by {@link BeanWrapper}; implemented by {@link BeanWrapperImpl}
 * and {@link org.springframework.validation.DataBinder}.
 *
 * @author Juergen Hoeller
 * @since 1.2.6
 * @see java.beans.PropertyEditor
 * @see PropertyEditorRegistrar
 * @see BeanWrapper
 * @see org.springframework.validation.DataBinder
 */
//PropertyEditorRegistrar里面的方法注册PropertyEditorRegistry
//PropertyEditorRegistry里面的方法注册PropertyEditor
//PropertyEditor属性编辑器的主要功能就是将外部的设置值转换为JVM内部的对应类型，所以属性编辑器其实就是一个类型转换器。
public interface PropertyEditorRegistry {

	/**
	 * Register the given custom property editor for all properties of the given type.
	 * @param requiredType the type of the property
	 * @param propertyEditor the editor to register
	 */
	void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor);

	/**
	 * Register the given custom property editor for the given type and
	 * property, or for all properties of the given type.
	 * <p>If the property path denotes an array or Collection property,
	 * the editor will get applied either to the array/Collection itself
	 * (the {@link PropertyEditor} has to create an array or Collection value) or
	 * to each element (the {@code PropertyEditor} has to create the element type),
	 * depending on the specified required type.
	 * <p>Note: Only one single registered custom editor per property path
	 * is supported. In the case of a Collection/array, do not register an editor
	 * for both the Collection/array and each element on the same property.
	 * <p>For example, if you wanted to register an editor for "items[n].quantity"
	 * (for all values n), you would use "items.quantity" as the value of the
	 * 'propertyPath' argument to this method.
	 * @param requiredType the type of the property. This may be {@code null}
	 * if a property is given but should be specified in any case, in particular in
	 * case of a Collection - making clear whether the editor is supposed to apply
	 * to the entire Collection itself or to each of its entries. So as a general rule:
	 * <b>Do not specify {@code null} here in case of a Collection/array!</b>
	 * @param propertyPath the path of the property (name or nested path), or
	 * {@code null} if registering an editor for all properties of the given type
	 * @param propertyEditor editor to register
	 */
    // 通过API：registerCustomEditor(...)放进此Map里（若指定了propertyPath）， 比如spring自带的一个测试用例，简要如下：

    // propertyPath ： 比如IndexedTestBean对象中有一个属性List<TestBean>  list， 属性TestBean里面有一个name属性
    // IndexedTestBean bean = new IndexedTestBean();
    // BeanWrapper bw = new BeanWrapperImpl(bean);
    // bw.registerCustomEditor(String.class, "list[0].name", PropertyEditorA);
    // 这样我们就为bw中的IndexedTestBean bean的属性List<TestBean>[0]的name属性注册了一个属性编辑器， spring会用它来转换PropertyValue中的属性值
    // bw.getPropertyValue("list[0].name") 通过这个方法，传入这个属性名，就可以获取到PropertyEditorA转换格式后的属性值
	void registerCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath, PropertyEditor propertyEditor);

	/**
	 * Find a custom property editor for the given type and property.
	 * @param requiredType the type of the property (can be {@code null} if a property
	 * is given but should be specified in any case for consistency checking)
	 * @param propertyPath the path of the property (name or nested path), or
	 * {@code null} if looking for an editor for all properties of the given type
	 * @return the registered editor, or {@code null} if none
	 */
	@Nullable
	PropertyEditor findCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath);

}

/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core.env;

import org.springframework.lang.Nullable;

/**
 * Holder containing one or more {@link PropertySource} objects.
 *
 * @author Chris Beams
 * @since 3.1
 * @see PropertySource
 */
/*
 * 所谓属性源，其实就是一个属性集合，它内部封装了多个key/value键值对，通过name可以获取与之对应的value值。
 * 比如我们经常在spring boot项目中创建的bootstrap.properties文件，在AbstractEnvironment.propertySources就会有一个名为applicationConfig:[classpath:bootstrap.properties]的propertysource对象(实现类里面会有属性保存bootstrap.properties的所有key-value)，
 *
 * PropertySource属性源对象通常不单独使用，而是通过一个PropertySources（注意s）对象，我称它为属性源集合对象，由这个对象来统一管理。
 * PropertySources其实就相当于一个Collection容器，其内部聚集了多个PropertySource属性源对象，且有序。
 * 它可以按序遍历内部持有的每个属性源，搜索name对应的value，找到即返回。可以用与占位符${...} spEL表达式#{...}等
 *
 * PropertySources属性源集合又跟PropertyResolver属性解决器协作，共同解决$ {}格式的属性占位符。
 *
 * 最后，总结一下PropertySource、PropertySources和PropertyResolver三者之间的关系：
 * PropertyResolver属性解决器可以处理嵌套结构的占位符，而占位符对应的的值来自于PropertySources属性源集合，PropertySources负责搜索内部的每个PropertySource（它才是属性值的真正保存者）。
 */
public interface PropertySources extends Iterable<PropertySource<?>> {

	/**
	 * Return whether a property source with the given name is contained.
	 * @param name the {@linkplain PropertySource#getName() name of the property source} to find
	 */
	boolean contains(String name);

	/**
	 * Return the property source with the given name, {@code null} if not found.
	 * @param name the {@linkplain PropertySource#getName() name of the property source} to find
	 */
	@Nullable
	PropertySource<?> get(String name);

}

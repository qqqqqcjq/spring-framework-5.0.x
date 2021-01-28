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

package org.springframework.beans.factory.config;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.io.support.PropertiesLoaderSupport;
import org.springframework.util.ObjectUtils;

/**
 * Allows for configuration of individual bean property values from a property resource,
 * i.e. a properties file. Useful for custom config files targeted at system
 * administrators that override bean properties configured in the application context.
 *
 * <p>Two concrete implementations are provided in the distribution:
 * <ul>
 * <li>{@link PropertyOverrideConfigurer} for "beanName.property=value" style overriding
 * (<i>pushing</i> values from a properties file into bean definitions)
 * <li>{@link PropertyPlaceholderConfigurer} for replacing "${...}" placeholders
 * (<i>pulling</i> values from a properties file into bean definitions)
 * </ul>
 *
 * <p>Property values can be converted after reading them in, through overriding
 * the {@link #convertPropertyValue} method. For example, encrypted values
 * can be detected and decrypted accordingly before processing them.
 *
 * @author Juergen Hoeller
 * @since 02.10.2003
 * @see PropertyOverrideConfigurer
 * @see PropertyPlaceholderConfigurer
 */
//从属性资源中配置bean属性(应该是配置到bd中的AbstractBeanDefinition.propertyValues)
public abstract class PropertyResourceConfigurer extends PropertiesLoaderSupport
		implements BeanFactoryPostProcessor, PriorityOrdered {

	private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered


	/**
	 * Set the order value of this object for sorting purposes.
	 * @see PriorityOrdered
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	/**
	 * {@linkplain #mergeProperties Merge}, {@linkplain #convertProperties convert} and
	 * {@linkplain #processProperties process} properties against the given bean factory.
	 * @throws BeanInitializationException if any properties cannot be loaded
	 */
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		try {
            // 合并本地属性和外部指定的属性文件资源中的属性(给bean的属性指定值(外部资源指定的值优先于xml中直接定义的值)  )
            // 该方法由基类PropertiesLoaderSupport提供缺省实现,用于合并本地属性和外来属性为一个Properties对象;
			Properties mergedProps = mergeProperties();

			// Convert the merged properties, if necessary.
            // 将属性的值做转换(仅在必要的时候做)
            // 该方法由PropertyResourceConfigurer自身提供缺省实现，用于对属性值做必要的转换处理，缺省不做任何处理
			convertProperties(mergedProps);

			// Let the subclass process the properties.
            // 对容器中的每个bean定义进行处理，也就是替换每个bean定义中的属性中的占位符，由子类实现
            //==============================PropertyOverrideConfigurer子类========================================================================
            // 用于处理"beanName.property=value"这种风格的属性值覆盖，将属性对象中的属性"推送(push)"到bean定义中
            // 比如对于一个有一个person.properties属性配置文件，如下引入spring中
            //<? xml version="1.0" encoding="UTF-8" ?>
            // <! DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN//EN" "http://www.springframework.org/dtd/spring-beans.dtd" >
            // < beans >
            //   < bean  id ="propertyConfigure"  class ="org.springframework.beans.factory.config.PropertyOverrideConfigurer" >
            //      < property  name ="locations"  value ="classpath:Bean/propertytwo/person.properties" ></ property >
            //   </ bean >
            //   < bean  id ="chinese"  class ="Bean.propertytwo.Chinese" >
            //       < property  name ="age"  value ="30" ></ property >
            //       < property  name ="name"  value ="gaoxiang" ></ property >
            //    </ bean >
            // </ beans >
            //person.properties配置文件中 chinese.age=26 那么就会覆盖xml中默认的属性值，最终可以看到bean实例中age属性是26
            //========================================PropertyPlaceholderConfigurer子类==============================================================
            // 用于处理bean定义中"${name}"这样的占位符解析,从属性对象中"拉取(pull)"到bean定义的属性值中
            //比如：
            //jdbc.properties 文件
            //jdbc.url=jdbc:mysql://66.59.208.106:3306/ds?useUnicode=true&amp;characterEncoding=utf-8&allowMultiQueries=true
            //jdbc.username=root
            //jdbc.password=root
            //.xml中引入外部文件，即.properties文件
            //<bean id="propertyConfigurer" class="org.springframework.beans.factory.config.PropertyPlaceholderConfigurer">
            //	<property name="locations">
            //		<value>jdbc.properties</value>
            //	</property>
            //</bean>
            //引入外部文件后，就可以在xml中用${key}替换指定的properties文件中的值，通常项目中都会将jdbc的配置放在properties文件中
            //<!-- 配置dbcp数据源 -->
            //<bean id="dataSourceDefault" class="org.apache.commons.dbcp.BasicDataSource">
            //	<property name="driverClassName" value="${jdbc.driverClassName}" />
            //	<property name="url" value="${jdbc.url}" />
            //	<property name="username" value="${jdbc.username}" />
            //	<property name="password" value="${jdbc.password}" />
            //</bean>
			processProperties(beanFactory, mergedProps);
		}
		catch (IOException ex) {
			throw new BeanInitializationException("Could not load properties", ex);
		}
	}

	/**
	 * Convert the given merged properties, converting property values
	 * if necessary. The result will then be processed.
	 * <p>The default implementation will invoke {@link #convertPropertyValue}
	 * for each property value, replacing the original with the converted value.
	 * @param props the Properties to convert
	 * @see #processProperties
	 */
	protected void convertProperties(Properties props) {
		Enumeration<?> propertyNames = props.propertyNames();
		while (propertyNames.hasMoreElements()) {
			String propertyName = (String) propertyNames.nextElement();
			String propertyValue = props.getProperty(propertyName);
			String convertedValue = convertProperty(propertyName, propertyValue);
			if (!ObjectUtils.nullSafeEquals(propertyValue, convertedValue)) {
				props.setProperty(propertyName, convertedValue);
			}
		}
	}

	/**
	 * Convert the given property from the properties source to the value
	 * which should be applied.
	 * <p>The default implementation calls {@link #convertPropertyValue(String)}.
	 * @param propertyName the name of the property that the value is defined for
	 * @param propertyValue the original value from the properties source
	 * @return the converted value, to be used for processing
	 * @see #convertPropertyValue(String)
	 */
	protected String convertProperty(String propertyName, String propertyValue) {
		return convertPropertyValue(propertyValue);
	}

	/**
	 * Convert the given property value from the properties source to the value
	 * which should be applied.
	 * <p>The default implementation simply returns the original value.
	 * Can be overridden in subclasses, for example to detect
	 * encrypted values and decrypt them accordingly.
	 * @param originalValue the original value from the properties source
	 * (properties file or local "properties")
	 * @return the converted value, to be used for processing
	 * @see #setProperties
	 * @see #setLocations
	 * @see #setLocation
	 * @see #convertProperty(String, String)
	 */
	protected String convertPropertyValue(String originalValue) {
		return originalValue;
	}


	/**
	 * Apply the given Properties to the given BeanFactory.
	 * @param beanFactory the BeanFactory used by the application context
	 * @param props the Properties to apply
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	protected abstract void processProperties(ConfigurableListableBeanFactory beanFactory, Properties props)
			throws BeansException;

}

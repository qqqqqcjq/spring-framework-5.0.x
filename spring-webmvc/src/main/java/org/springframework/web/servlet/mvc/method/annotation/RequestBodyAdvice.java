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

package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.lang.reflect.Type;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Allows customizing the request before its body is read and converted into an
 * Object and also allows for processing of the resulting Object before it is
 * passed into a controller method as an {@code @RequestBody} or an
 * {@code HttpEntity} method argument.
 *
 * <p>Implementations of this contract may be registered directly with the
 * {@code RequestMappingHandlerAdapter} or more likely annotated with
 * {@code @ControllerAdvice} in which case they are auto-detected.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
//在实际项目中 , 往往需要对请求body做一些统一的操作 , 例如body的过滤 , 字符的编码 , 第三方的解密等等 , Spring提供了RequestBodyAdvice一个全局的解决方案 , 免去了我们在Controller处理的繁琐 .
//提供一个接口，对请求的body在不同的节点进行处理
//比如 ：
//@ControllerAdvice(basePackages = "com.xbz.controller")//此处设置需要GlobalRequestBodyAdvice生效的范围, 省略默认全局生效
//public class GlobalRequestBodyAdvice implements RequestBodyAdvice
public interface RequestBodyAdvice {

	/**
	 * Invoked first to determine if this interceptor applies.
	 * @param methodParameter the method parameter
	 * @param targetType the target type, not necessarily the same as the method
	 * parameter type, e.g. for {@code HttpEntity<String>}.
	 * @param converterType the selected converter type
	 * @return whether this interceptor should be invoked or not
	 */
	//首先调用，以确定是否应用此拦截器。
	boolean supports(MethodParameter methodParameter, Type targetType,
			Class<? extends HttpMessageConverter<?>> converterType);

	/**
	 * Invoked second before the request body is read and converted.
	 * @param inputMessage the request
	 * @param parameter the target method parameter
	 * @param targetType the target type, not necessarily the same as the method
	 * parameter type, e.g. for {@code HttpEntity<String>}.
	 * @param converterType the converter used to deserialize the body
	 * @return the input request or a new instance, never {@code null}
	 */
	//在读取和转换请求主体(body)之前第二次调用
	HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
			Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException;

	/**
	 * Invoked third (and last) after the request body is converted to an Object.
	 * @param body set to the converter Object before the first advice is called
	 * @param inputMessage the request
	 * @param parameter the target method parameter
	 * @param targetType the target type, not necessarily the same as the method
	 * parameter type, e.g. for {@code HttpEntity<String>}.
	 * @param converterType the converter used to deserialize the body
	 * @return the same body or a new instance
	 */
	//在请求主体(body)转换为要求的对象之后第三次(也是最后一次)调用
	Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
			Type targetType, Class<? extends HttpMessageConverter<?>> converterType);

	/**
	 * Invoked second (and last) if the body is empty.
	 * @param body usually set to {@code null} before the first advice is called
	 * @param inputMessage the request
	 * @param parameter the method parameter
	 * @param targetType the target type, not necessarily the same as the method
	 * parameter type, e.g. for {@code HttpEntity<String>}.
	 * @param converterType the selected converter type
	 * @return the value to use or {@code null} which may then raise an
	 * {@code HttpMessageNotReadableException} if the argument is required.
	 */
	//如果主体为空，则第二次(也是最后一次)调用
	@Nullable
	Object handleEmptyBody(@Nullable Object body, HttpInputMessage inputMessage, MethodParameter parameter,
			Type targetType, Class<? extends HttpMessageConverter<?>> converterType);


}

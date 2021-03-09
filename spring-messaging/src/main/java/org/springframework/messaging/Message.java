/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.messaging;

/**
 * A generic message representation with headers and body.
 *
 * @author Mark Fisher
 * @author Arjen Poutsma
 * @since 4.0
 * @see org.springframework.messaging.support.MessageBuilder
 */
//spring-messaging：用于构建基于消息的应用程序(这里所说的消息就是广义的消息，不是单纯的指AMQP，当然，AMQP可以遵循spring-messaging接口标准，将自己的AMQP组件加入到spring生态)
//消息 Message 对应的模型就包括一个消息体 Payload 和消息头 Header:
public interface Message<T> {

	/**
	 * Return the message payload.
	 */
	T getPayload();

	/**
	 * Return message headers for the message (never {@code null} but may be empty).
	 */
	MessageHeaders getHeaders();

}

/**
 * Spring Messaging 内部在消息模型的基础上衍生出了其它的一些功能，如：
 * 1. 消息接收参数及返回值处理：消息接收参数处理器 HandlerMethodArgumentResolver 配合 @Header, @Payload 等注解使用；消息接收后的返回值处理器 HandlerMethodReturnValueHandler 配合 @SendTo 注解使用；
 *
 * 2. 消息体内容转换器 MessageConverter；
 *
 * 3. 统一抽象的消息发送模板 AbstractMessageSendingTemplate；
 *
 * 4. 消息通道拦截器 ChannelInterceptor；
 */

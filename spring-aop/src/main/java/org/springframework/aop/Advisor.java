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

package org.springframework.aop;

import org.aopalliance.aop.Advice;

/**
 * Base interface holding AOP <b>advice</b> (action to take at a joinpoint)
 * and a filter determining the applicability of the advice (such as
 * a pointcut). <i>This interface is not for use by Spring users, but to
 * allow for commonality in support for different types of advice.</i>
 *
 * <p>Spring AOP is based around <b>around advice</b> delivered via method
 * <b>interception</b>, compliant with the AOP Alliance interception API.
 * The Advisor interface allows support for different types of advice,
 * such as <b>before</b> and <b>after</b> advice, which need not be
 * implemented using interception.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
//Advisor是Advice(通知)和PointCut(切入点)的一个组合体，按照aop的定义其就是一个切面。

/*
<aop:config>
<aop:aspect ref="transaction">
<aop:before method="startTransaction" pointcut="execution(* *.save(..))"/>
<aop:after method="commitTransaction" pointcut="execution(* *.save(..))"/>
<aop:after-throwing method="rollbackTransaction" pointcut="execution(* *.save(..))"/>
<aop:after-returning method="commitTransaction" pointcut="execution(* *.save(..))"/>
</aop:aspect>
</aop:config>


1、pointcut : 代表切入点的匹配表达式(pointcut就是连接点joinpoint的集合, 匹配表达式匹配后就可以得到joinpoint的集合)
2、advice : before、after等加上method表示每个通知
3、Advisor : before加上method加上pointcut就可以作为一个Advisor切面
4、<aop:aspect> : 代表一个切面Advisor集合
5、ref=“transaction”  : 表示startTransaction/commitTransaction/rollbackTransaction/commitTransaction这几个通知方法在transaction这个类里面定义
这样通过pointcut的匹配表达式，可以将Advice横切到目标类的方法中

 */

public interface Advisor {

	/**
	 * Common placeholder for an empty {@code Advice} to be returned from
	 * {@link #getAdvice()} if no proper advice has been configured (yet).
	 * @since 5.0
	 */
	Advice EMPTY_ADVICE = new Advice() {};


	/**
	 * Return the advice part of this aspect. An advice may be an
	 * interceptor, a before advice, a throws advice, etc.
	 * @return the advice that should apply if the pointcut matches
	 * @see org.aopalliance.intercept.MethodInterceptor
	 * @see BeforeAdvice
	 * @see ThrowsAdvice
	 * @see AfterReturningAdvice
	 */
	Advice getAdvice();

	/**
	 * Return whether this advice is associated with a particular instance
	 * (for example, creating a mixin) or shared with all instances of
	 * the advised class obtained from the same Spring bean factory.
	 * <p><b>Note that this method is not currently used by the framework.</b>
	 * Typical Advisor implementations always return {@code true}.
	 * Use singleton/prototype bean definitions or appropriate programmatic
	 * proxy creation to ensure that Advisors have the correct lifecycle model.
	 * @return whether this advice is associated with a particular target instance
	 */
	boolean isPerInstance();

}

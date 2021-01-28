/*
 * Copyright 2002-2016 the original author or authors.
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

package org.aopalliance.aop;

/**
 * Tag interface for Advice. Implementations can be any type
 * of advice, such as Interceptors.
 *
 * @author Rod JohnsonHandlerInterceptor
 * @version $Id: Advice.java,v 1.1 2004/03/19 17:02:16 johnsonr Exp $
 */

//通知 : 拦截位置，以及需要增强的逻辑
public interface Advice {

}


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


注意区分 ： Advised这个接口定义了Interceptors(属于advice) 其他advice,  Adivisors ,代理的接口，由AdvisedSupport实现，直接实现类也只有AdvisedSupport

 */
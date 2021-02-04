/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.aop.framework;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetClassAware;
import org.springframework.aop.TargetSource;

/**
 * Interface to be implemented by classes that hold the configuration  of a factory of AOP proxies.
 * This configuration includes the Interceptors and other advice, Advisors, and the proxied interfaces.
 *
 * <p>Any AOP proxy obtained from Spring can be cast to this interface to
 * allow manipulation of its AOP advice.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 13.03.2003
 * @see org.springframework.aop.framework.AdvisedSupport
 */
// Advised这个接口的实现类封装了Interceptor(属于advice)，其他advice,  Adivisors ,代理的接口，
// 由AdvisedSupport实现，直接实现类也只有AdvisedSupport
/**
 * 是生成代理的一个重要的接口定义, 继承关系如下：
 * ===============================begin=============================================
 *                                                          <==  AspectJProxyFactory
 * Advised  <==  AdvisedSupport  <==  ProxyCreatorSupport   <==  ProxyFactoryBean
 *                                                          <==  ProxyFactory
 * ===============================end  =============================================
 */

public interface Advised extends TargetClassAware {

	/**
	 * Return whether the Advised configuration is frozen,
	 * in which case no advice changes can be made.
	 */
	boolean isFrozen();

	/**
	 * Are we proxying the full target class instead of specified interfaces?
	 */
	boolean isProxyTargetClass();

	/**
	 * Return the interfaces proxied by the AOP proxy.
	 * <p>Will not include the target class, which may also be proxied.
	 */
	Class<?>[] getProxiedInterfaces();

	/**
	 * Determine whether the given interface is proxied.
	 * @param intf the interface to check
	 */
	boolean isInterfaceProxied(Class<?> intf);

	/**
	 * Change the {@code TargetSource} used by this {@code Advised} object.
	 * <p>Only works if the configuration isn't {@linkplain #isFrozen frozen}.
	 * @param targetSource new TargetSource to use
	 */
	void setTargetSource(TargetSource targetSource);

	/**
	 * Return the {@code TargetSource} used by this {@code Advised} object.
	 */
	TargetSource getTargetSource();

	/**
	 * Set whether the proxy should be exposed by the AOP framework as a
	 * {@link ThreadLocal} for retrieval via the {@link AopContext} class.
	 * <p>It can be necessary to expose the proxy if an advised object needs
	 * to invoke a method on itself with advice applied. Otherwise, if an
	 * advised object invokes a method on {@code this}, no advice will be applied.
	 * <p>Default is {@code false}, for optimal performance.
	 */
	void setExposeProxy(boolean exposeProxy);

	/**
	 * Return whether the factory should expose the proxy as a {@link ThreadLocal}.
	 * <p>It can be necessary to expose the proxy if an advised object needs
	 * to invoke a method on itself with advice applied. Otherwise, if an
	 * advised object invokes a method on {@code this}, no advice will be applied.
	 * <p>Getting the proxy is analogous to an EJB calling {@code getEJBObject()}.
	 * @see AopContext
	 */
	boolean isExposeProxy();

	/**
	 * Set whether this proxy configuration is pre-filtered so that it only
	 * contains applicable advisors (matching this proxy's target class).
	 * <p>Default is "false". Set this to "true" if the advisors have been
	 * pre-filtered already, meaning that the ClassFilter check can be skipped
	 * when building the actual advisor chain for proxy invocations.
	 * @see org.springframework.aop.ClassFilter
	 */
	void setPreFiltered(boolean preFiltered);

	/**
	 * Return whether this proxy configuration is pre-filtered so that it only
	 * contains applicable advisors (matching this proxy's target class).
	 */
	boolean isPreFiltered();

	/**
	 * Return the advisors applying to this proxy.
	 * @return a list of Advisors applying to this proxy (never {@code null})
	 */
	Advisor[] getAdvisors();

	/**
	 * Add an advisor at the end of the advisor chain.
	 * <p>The Advisor may be an {@link org.springframework.aop.IntroductionAdvisor},
	 * in which new interfaces will be available when a proxy is next obtained
	 * from the relevant factory.
	 * @param advisor the advisor to add to the end of the chain
	 * @throws AopConfigException in case of invalid advice
	 */
	void addAdvisor(Advisor advisor) throws AopConfigException;

	/**
	 * Add an Advisor at the specified position in the chain.
	 * @param advisor the advisor to add at the specified position in the chain
	 * @param pos position in chain (0 is head). Must be valid.
	 * @throws AopConfigException in case of invalid advice
	 */
	void addAdvisor(int pos, Advisor advisor) throws AopConfigException;

	/**
	 * Remove the given advisor.
	 * @param advisor the advisor to remove
	 * @return {@code true} if the advisor was removed; {@code false}
	 * if the advisor was not found and hence could not be removed
	 */
	boolean removeAdvisor(Advisor advisor);

	/**
	 * Remove the advisor at the given index.
	 * @param index index of advisor to remove
	 * @throws AopConfigException if the index is invalid
	 */
	void removeAdvisor(int index) throws AopConfigException;

	/**
	 * Return the index (from 0) of the given advisor,
	 * or -1 if no such advisor applies to this proxy.
	 * <p>The return value of this method can be used to index into the advisors array.
	 * @param advisor the advisor to search for
	 * @return index from 0 of this advisor, or -1 if there's no such advisor
	 */
	int indexOf(Advisor advisor);

	/**
	 * Replace the given advisor.
	 * <p><b>Note:</b> If the advisor is an {@link org.springframework.aop.IntroductionAdvisor}
	 * and the replacement is not or implements different interfaces, the proxy will need
	 * to be re-obtained or the old interfaces won't be supported and the new interface
	 * won't be implemented.
	 * @param a the advisor to replace
	 * @param b the advisor to replace it with
	 * @return whether it was replaced. If the advisor wasn't found in the
	 * list of advisors, this method returns {@code false} and does nothing.
	 * @throws AopConfigException in case of invalid advice
	 */
	boolean replaceAdvisor(Advisor a, Advisor b) throws AopConfigException;

	/**
	 * Add the given AOP Alliance advice to the tail of the advice (interceptor) chain.
	 * <p>This will be wrapped in a DefaultPointcutAdvisor with a pointcut that always
	 * applies, and returned from the {@code getAdvisors()} method in this wrapped form.
	 * <p>Note that the given advice will apply to all invocations on the proxy,
	 * even to the {@code toString()} method! Use appropriate advice implementations
	 * or specify appropriate pointcuts to apply to a narrower set of methods.
	 * @param advice advice to add to the tail of the chain
	 * @throws AopConfigException in case of invalid advice
	 * @see #addAdvice(int, Advice)
	 * @see org.springframework.aop.support.DefaultPointcutAdvisor
	 */
	void addAdvice(Advice advice) throws AopConfigException;

	/**
	 * Add the given AOP Alliance Advice at the specified position in the advice chain.
	 * <p>This will be wrapped in a {@link org.springframework.aop.support.DefaultPointcutAdvisor}
	 * with a pointcut that always applies, and returned from the {@link #getAdvisors()}
	 * method in this wrapped form.
	 * <p>Note: The given advice will apply to all invocations on the proxy,
	 * even to the {@code toString()} method! Use appropriate advice implementations
	 * or specify appropriate pointcuts to apply to a narrower set of methods.
	 * @param pos index from 0 (head)
	 * @param advice advice to add at the specified position in the advice chain
	 * @throws AopConfigException in case of invalid advice
	 */
	void addAdvice(int pos, Advice advice) throws AopConfigException;

	/**
	 * Remove the Advisor containing the given advice.
	 * @param advice the advice to remove
	 * @return {@code true} of the advice was found and removed;
	 * {@code false} if there was no such advice
	 */
	boolean removeAdvice(Advice advice);

	/**
	 * Return the index (from 0) of the given AOP Alliance Advice,
	 * or -1 if no such advice is an advice for this proxy.
	 * <p>The return value of this method can be used to index into
	 * the advisors array.
	 * @param advice AOP Alliance advice to search for
	 * @return index from 0 of this advice, or -1 if there's no such advice
	 */
	int indexOf(Advice advice);

	/**
	 * As {@code toString()} will normally be delegated to the target,
	 * this returns the equivalent for the AOP proxy.
	 * @return a string description of the proxy configuration
	 */
	String toProxyConfigString();

}


// <aop:config>
// <aop:aspect ref="transaction">
// <aop:before method="startTransaction" pointcut="execution(* *.save(..))"/>
// <aop:after method="commitTransaction" pointcut="execution(* *.save(..))"/>
// <aop:after-throwing method="rollbackTransaction" pointcut="execution(* *.save(..))"/>
// <aop:after-returning method="commitTransaction" pointcut="execution(* *.save(..))"/>
// </aop:aspect>
// </aop:config>

// @Aspect
// @Component
// public class AopAspect {
//     /**
//      * 定义一个切入点表达式,用来确定哪些类需要代理
//      * execution(* lubanaop.anothersample1.*.*(..))代表lubanaop.anothersample1包下所有类的所有方法都会被代理
//      */
//     @Pointcut("execution(* lubanaop.anothersample1.*.*(..))")
//     public void declareJoinPointerExpression() {}
//
//     /**
//      * 前置方法,在目标方法执行前执行
//      * @param joinPoint 封装了代理方法信息的对象,若用不到则可以忽略不写
//      */
//     @Before("declareJoinPointerExpression()")
//     public void beforeMethod(JoinPoint joinPoint){
//         System.out.println("目标方法名为:" + joinPoint.getSignature().getName());
//         System.out.println("目标方法所属类的简单类名:" +        joinPoint.getSignature().getDeclaringType().getSimpleName());
//         System.out.println("目标方法所属类的类名:" + joinPoint.getSignature().getDeclaringTypeName());
//         System.out.println("目标方法声明类型:" + Modifier.toString(joinPoint.getSignature().getModifiers()));
//         //获取传入目标方法的参数
//         Object[] args = joinPoint.getArgs();
//         for (int i = 0; i < args.length; i++) {
//             System.out.println("第" + (i+1) + "个参数为:" + args[i]);
//         }
//         System.out.println("被代理的对象:" + joinPoint.getTarget());
//         System.out.println("代理对象自己:" + joinPoint.getThis());
//     }
//
//     /**
//      * 环绕方法,可自定义目标方法执行的时机
//      * @param pjd JoinPoint的子接口,添加了
//      *            Object proceed() throws Throwable 执行目标方法
//      *            Object proceed(Object[] var1) throws Throwable 传入的新的参数去执行目标方法
//      *            两个方法
//      * @return 此方法需要返回值,返回值视为目标方法的返回值
//      */
//     @Around("declareJoinPointerExpression()")
//     public Object aroundMethod(ProceedingJoinPoint pjd){
//         Object result = null;
//
//         try {
//             //前置通知
//             System.out.println("目标方法执行前...");
//             //执行目标方法
//             //result = pjd.proeed();
//             //用新的参数值执行目标方法
//             result = pjd.proceed(new Object[]{"newSpring","newAop"});
//             //返回通知
//             System.out.println("目标方法返回结果后...");
//         } catch (Throwable e) {
//             //异常通知
//             System.out.println("执行目标方法异常后...");
//             throw new RuntimeException(e);
//         }
//         //后置通知
//         System.out.println("目标方法执行后...");
//
//         return result;
//     }
// }


// 1、Pointcut : 代表切入点的匹配表达式(Pointcut就是连接点Joinpoint的集合, 匹配表达式匹配后就可以得到Joinpoint的集合)
// 2、Advice : before、after等加上method表示每个通知
// 3、Advisor : before、after等加上method加上Pointcut就可以作为一个Advisor切面
// 4、<aop:aspect> : 代表一个切面Advisor集合
// 5、ref=“transaction”  : 表示startTransaction/commitTransaction/rollbackTransaction/commitTransaction这几个通知方法在transaction这个类里面定义
// 这样通过Pointcut的匹配表达式，可以将Advice横切到目标类的方法中
//
//
// 注意区分 ： Advised这个接口定义了Interceptor(继承自Advice) 其他Advice,  Adivisors ,代理的接口，由AdvisedSupport实现，直接实现类也只有AdvisedSupport


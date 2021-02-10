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

package org.springframework.aop;

import java.lang.reflect.Method;

import org.springframework.lang.Nullable;

/**
 * Part of a {@link Pointcut}: Checks whether the target method is eligible for advice.
 *
 * <p>A MethodMatcher may be evaluated <b>statically</b> or at <b>runtime</b> (dynamically).
 * Static matching involves method and (possibly) method attributes. Dynamic matching
 * also makes arguments for a particular call available, and any effects of running
 * previous advice applying to the joinpoint.
 *
 * <p>If an implementation returns {@code false} from its {@link #isRuntime()}
 * method, evaluation can be performed statically, and the result will be the same
 * for all invocations of this method, whatever their arguments. This means that
 * if the {@link #isRuntime()} method returns {@code false}, the 3-arg
 * {@link #matches(java.lang.reflect.Method, Class, Object[])} method will never be invoked.
 *
 * <p>If an implementation returns {@code true} from its 2-arg
 * {@link #matches(java.lang.reflect.Method, Class)} method and its {@link #isRuntime()} method
 * returns {@code true}, the 3-arg {@link #matches(java.lang.reflect.Method, Class, Object[])}
 * method will be invoked <i>immediately before each potential execution of the related advice</i>,
 * to decide whether the advice should run. All previous advice, such as earlier interceptors
 * in an interceptor chain, will have run, so any state changes they have produced in
 * parameters or ThreadLocal state will be available at the time of evaluation.
 *
 * @author Rod Johnson
 * @since 11.11.2003
 * @see Pointcut
 * @see ClassFilter
 */

//Advisor中可以获取PointCut,PointCut可以获取MethodMatcher，使用MethodMatcher判断方法是否满足当前PointCut
//切入点的一部分，用于检查目标方法是否符合通知的条件
public interface MethodMatcher {

	/**
	 * Perform static checking whether the given method matches.
	 * <p>If this returns {@code false} or if the {@link #isRuntime()}
	 * method returns {@code false}, no runtime check (i.e. no
	 * {@link #matches(java.lang.reflect.Method, Class, Object[])} call)
	 * will be made.
	 * @param method the candidate method 候选方法
	 * @param targetClass the target class (may be {@code null}, in which case
	 * the candidate class must be taken to be the method's declaring class)
	 * @return whether or not this method matches statically
	 */
    //isRuntime()为false, 对给定的方法进行动态匹配,参数如下：
    //method ： 给定方法
    //targetClass : 目标类
	boolean matches(Method method, @Nullable Class<?> targetClass);

	/**
	 * Is this MethodMatcher dynamic, that is, must a final call be made on the
	 * {@link #matches(java.lang.reflect.Method, Class, Object[])} method at
	 * runtime even if the 2-arg matches method returns {@code true}?
	 * <p>Can be invoked when an AOP proxy is created, and need not be invoked
	 * again before each method invocation,
	 * @return whether or not a runtime match via the 3-arg
	 * {@link #matches(java.lang.reflect.Method, Class, Object[])} method
	 * is required if static matching passed
	 */
	boolean isRuntime();

	/**
	 * Check whether there a runtime (dynamic) match for this method,
	 * which must have matched statically.
	 * <p>This method is invoked only if the 2-arg matches method returns
	 * {@code true} for the given method and target class, and if the
	 * {@link #isRuntime()} method returns {@code true}. Invoked
	 * immediately before potential running of the advice, after any
	 * advice earlier in the advice chain has run.
	 * @param method the candidate method
	 * @param targetClass the target class (may be {@code null}, in which case
	 * the candidate class must be taken to be the method's declaring class)
	 * @param args arguments to the method
	 * @return whether there's a runtime match
	 * @see MethodMatcher#matches(Method, Class)
	 */
	//isRuntime()为true, 对给定的方法进行动态匹配,参数如下：
    //method ： 给定方法
    //targetClass : 目标类
    //args : 运行时参数
	boolean matches(Method method, @Nullable Class<?> targetClass, Object... args);


	/**
	 * Canonical instance that matches all methods.
	 */
	MethodMatcher TRUE = TrueMethodMatcher.INSTANCE;

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


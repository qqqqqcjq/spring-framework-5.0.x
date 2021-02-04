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

package org.springframework.aop;

/**
 * Core Spring pointcut abstraction.
 *
 * <p>A pointcut is composed of a {@link ClassFilter} and a {@link MethodMatcher}.
 * Both these basic terms and a Pointcut itself can be combined to build up combinations
 * (e.g. through {@link org.springframework.aop.support.ComposablePointcut}).
 *
 * @author Rod Johnson
 * @see ClassFilter
 * @see MethodMatcher
 * @see org.springframework.aop.support.Pointcuts
 * @see org.springframework.aop.support.ClassFilters
 * @see org.springframework.aop.support.MethodMatchers
 */
//切入点 ： 表示连接点(Joinpoint)的集合

public interface Pointcut {

	/**
	 * Return the ClassFilter for this pointcut.
	 * @return the ClassFilter (never {@code null})
	 */
	ClassFilter getClassFilter();

	/**
	 * Return the MethodMatcher for this pointcut.
	 * @return the MethodMatcher (never {@code null})
	 */
	MethodMatcher getMethodMatcher();


	/**
	 * Canonical Pointcut instance that always matches.
	 */
	Pointcut TRUE = TruePointcut.INSTANCE;

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



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

package org.springframework.aop.aspectj.annotation;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.aopalliance.aop.Advice;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.DeclareParents;
import org.aspectj.lang.annotation.Pointcut;

import org.springframework.aop.Advisor;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.aspectj.AbstractAspectJAdvice;
import org.springframework.aop.aspectj.AspectJAfterAdvice;
import org.springframework.aop.aspectj.AspectJAfterReturningAdvice;
import org.springframework.aop.aspectj.AspectJAfterThrowingAdvice;
import org.springframework.aop.aspectj.AspectJAroundAdvice;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.AspectJMethodBeforeAdvice;
import org.springframework.aop.aspectj.DeclareParentsAdvisor;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConvertingComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.comparator.InstanceComparator;

/**
 * Factory that can create Spring AOP Advisors given AspectJ classes from
 * classes honoring the AspectJ 5 annotation syntax, using reflection to
 * invoke the corresponding advice methods.
 *
 * @author Rod Johnson
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @author Phillip Webb
 * @since 2.0
 */
@SuppressWarnings("serial")
public class ReflectiveAspectJAdvisorFactory extends AbstractAspectJAdvisorFactory implements Serializable {

	private static final Comparator<Method> METHOD_COMPARATOR;

	static {
		Comparator<Method> adviceKindComparator = new ConvertingComparator<>(
				new InstanceComparator<>(
						Around.class, Before.class, After.class, AfterReturning.class, AfterThrowing.class),
				(Converter<Method, Annotation>) method -> {
					AspectJAnnotation<?> annotation =
						AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(method);
					return (annotation != null ? annotation.getAnnotation() : null);
				});
		Comparator<Method> methodNameComparator = new ConvertingComparator<>(Method::getName);
		METHOD_COMPARATOR = adviceKindComparator.thenComparing(methodNameComparator);
	}


	@Nullable
	private final BeanFactory beanFactory;


	/**
	 * Create a new {@code ReflectiveAspectJAdvisorFactory}.
	 */
	public ReflectiveAspectJAdvisorFactory() {
		this(null);
	}

	/**
	 * Create a new {@code ReflectiveAspectJAdvisorFactory}, propagating the given
	 * {@link BeanFactory} to the created {@link AspectJExpressionPointcut} instances,
	 * for bean pointcut handling as well as consistent {@link ClassLoader} resolution.
	 * @param beanFactory the BeanFactory to propagate (may be {@code null}}
	 * @since 4.3.6
	 * @see AspectJExpressionPointcut#setBeanFactory
	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#getBeanClassLoader()
	 */
	public ReflectiveAspectJAdvisorFactory(@Nullable BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
    /**
     * 分析这个方法以及其调用的方法可以明白获取Advisor的过程如下:
     * 循环切面类中的所有不带@Pointcut注解的方法，
     * 接着判断切面类的方法上是否有：@Before, @Around, @After, @AfterReturning, @AfterThrowing, @Pointcut注解。
     *     如果没有的话，循环下一个方法。
     *     如果有这些注解的话，则从这些注解中获取切点表达式存放到AspectJExpressionPointcut对象中，
     * 最后将获取相关信息封装到InstantiationModelAwarePointcutAdvisorImpl这个类中作为Adviosr
     */
	public List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory aspectInstanceFactory) {
		Class<?> aspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
		String aspectName = aspectInstanceFactory.getAspectMetadata().getAspectName();
		validate(aspectClass);

		// We need to wrap the MetadataAwareAspectInstanceFactory with a decorator
		// so that it will only instantiate once.
		MetadataAwareAspectInstanceFactory lazySingletonAspectInstanceFactory =
				new LazySingletonAspectInstanceFactoryDecorator(aspectInstanceFactory);

		List<Advisor> advisors = new ArrayList<>();
		//getAdvisorMethods(aspectClass)获取切面中不带@Pointcut注解的方法，@Pointcut注解的方法是连接点，切面类中的其他方法是横切方法，比如@Before @Around注解的方法
        //method:横切方法(切面中不带@Pointcut注解的方法)
        //lazySingletonAspectInstanceFactory:中含有切面实例 是个单例
        //advisors.size():要生成的advisor在advisors中的下标，指示切面的一个顺序
        //aspectName:切面类的名字
		for (Method method : getAdvisorMethods(aspectClass)) {
		    //可见一个横切方法逻辑method对应一个Advisor
            //生成这个Advisor实例的过程中会创建一个相应的Advice实例! 一个通知方法(横切方法)—->一个Advisor(包含Pointcut)——>一个Advice
			Advisor advisor = getAdvisor(method, lazySingletonAspectInstanceFactory, advisors.size(), aspectName);
			if (advisor != null) {
				advisors.add(advisor);
			}
		}

		// If it's a per target aspect, emit the dummy instantiating aspect.
		if (!advisors.isEmpty() && lazySingletonAspectInstanceFactory.getAspectMetadata().isLazilyInstantiated()) {
			Advisor instantiationAdvisor = new SyntheticInstantiationAdvisor(lazySingletonAspectInstanceFactory);
			advisors.add(0, instantiationAdvisor);
		}

		// Find introduction fields.
		for (Field field : aspectClass.getDeclaredFields()) {
			Advisor advisor = getDeclareParentsAdvisor(field);
			if (advisor != null) {
				advisors.add(advisor);
			}
		}

		return advisors;
	}

    //getAdvisorMethods(aspectClass)获取切面中不带@Pointcut注解的方法，@Pointcut注解的方法是连接点，切面类中的其他方法是横切方法，比如@Before @Around注解的方法
    private List<Method> getAdvisorMethods(Class<?> aspectClass) {
		final List<Method> methods = new ArrayList<>();
		ReflectionUtils.doWithMethods(aspectClass, method -> {
			// Exclude pointcuts
			if (AnnotationUtils.getAnnotation(method, Pointcut.class) == null) {
				methods.add(method);
			}
		});
		methods.sort(METHOD_COMPARATOR);
		return methods;
	}

	/**
	 * Build a {@link org.springframework.aop.aspectj.DeclareParentsAdvisor}
	 * for the given introduction field.
	 * <p>Resulting Advisors will need to be evaluated for targets.
	 * @param introductionField the field to introspect
	 * @return the Advisor instance, or {@code null} if not an Advisor
	 */
	@Nullable
	private Advisor getDeclareParentsAdvisor(Field introductionField) {
		DeclareParents declareParents = introductionField.getAnnotation(DeclareParents.class);
		if (declareParents == null) {
			// Not an introduction field
			return null;
		}

		if (DeclareParents.class == declareParents.defaultImpl()) {
			throw new IllegalStateException("'defaultImpl' attribute must be set on DeclareParents");
		}

		return new DeclareParentsAdvisor(
				introductionField.getType(), declareParents.value(), declareParents.defaultImpl());
	}


	@Override
	@Nullable

	public Advisor getAdvisor(Method candidateAdviceMethod, MetadataAwareAspectInstanceFactory aspectInstanceFactory,
			int declarationOrderInAspect, String aspectName) {

        //验证是否是切面类
		validate(aspectInstanceFactory.getAspectMetadata().getAspectClass());

        //这里根据传入的方法和切面类获取AspectJExpressionPointcut
		AspectJExpressionPointcut expressionPointcut = getPointcut(
				candidateAdviceMethod, aspectInstanceFactory.getAspectMetadata().getAspectClass());
        //没有获取到AspectJExpressionPointcut， 直接返回null
		if (expressionPointcut == null) {
			return null;
		}

        /**
         * Advisor有2类实现。1. PointcutAdvisor ：关联了Advice和Pointcut 2.IntroductionAdvisor ：关联了Advice和引介点
         * 返回一个Advisor的实例，这个实例中包含了 以下内容：
         * Advice实例：调用InstantiationModelAwarePointcutAdvisorImpl构造函数时会创建Advice实例
         * PointCut实例
         * 切点表达式 AspectJExpressionPointcut
         * 横切方法
         * ReflectiveAspectJAdvisorFactory实例
         * 切面类实例
         * 切面类名字
         */
		return new InstantiationModelAwarePointcutAdvisorImpl(expressionPointcut, candidateAdviceMethod,
				this, aspectInstanceFactory, declarationOrderInAspect, aspectName);
	}

	@Nullable
    //从调用链来看，前面只会传进来不带@Pointcut注解的方法candidateAdviceMethod，所以AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod)
    //检测后可以确定candidateAdviceMethod只有@Before, @Around, @After, @AfterReturning, @AfterThrowing中的一个。Spring应该是想直接使用AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod()方法，不去再写一个类似的方法
    //，所以前面就把@Pointcut注解的方法排除了，到这里再调用AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod()就可以确认方法candidateAdviceMethod只有@Before, @Around, @After, @AfterReturning, @AfterThrowing中的一个
	private AspectJExpressionPointcut getPointcut(Method candidateAdviceMethod, Class<?> candidateAspectClass) {
        //AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod()用来确认方法是否有@Before, @Around, @After, @AfterReturning, @AfterThrowing, @Pointcut注解
        //这里也提供了一种获取类上是否有我们想要的注解的一种方式，返回一个AspectJAnnotation对象，这里用了AnnotationUtils用来获取注解的相关信息
	    AspectJAnnotation<?> aspectJAnnotation =
				AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
		if (aspectJAnnotation == null) {
			return null;
		}

        //创建AspectJExpressionPointcut对象
		AspectJExpressionPointcut ajexp =
				new AspectJExpressionPointcut(candidateAspectClass, new String[0], new Class<?>[0]);

		//以@Before("declareJoinPointerExpression()")为例，设置切面表达式为declareJoinPointerExpression()。
        //在AspectJAnnotation中是可以获取到通知类型的(@Before)，但是这里没有设置通知类型,
        //在创建Advice的方法ReflectiveAspectJAdvisorFactory.getAdvice()中，重新调用AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod()方法，
        //获取AspectJAnnotation，进而获取通知类型。
		ajexp.setExpression(aspectJAnnotation.getPointcutExpression());
		if (this.beanFactory != null) {
			ajexp.setBeanFactory(this.beanFactory);
		}
		return ajexp;
	}


	@Override
	@Nullable
    // 这个方法根据AspectJ的声明创建Advice
    /**
     * AspectJAroundAdvice
     * AspectJMethodBeforeAdvice
     * AspectJAfterAdvice
     * AspectJAfterReturningAdvice
     * AspectJAfterThrowingAdvice
     */
	public Advice getAdvice(Method candidateAdviceMethod, AspectJExpressionPointcut expressionPointcut,
			MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName) {

        //获取切面类
		Class<?> candidateAspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
        //验证是否是切面类，带有@Aspect注解
		validate(candidateAspectClass);

		//根据横切父亲方法上的注解(@Before,@After等)，获取对应的AspectJAnnotation
		AspectJAnnotation<?> aspectJAnnotation =
				AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
		if (aspectJAnnotation == null) {
			return null;
		}

		// If we get here, we know we have an AspectJ method.
		// Check that it's an AspectJ-annotated class
        //再校验一遍，如果不是切面类，则抛出异常
		if (!isAspect(candidateAspectClass)) {
			throw new AopConfigException("Advice must be declared inside an aspect type: " +
					"Offending method '" + candidateAdviceMethod + "' in class [" +
					candidateAspectClass.getName() + "]");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Found AspectJ method: " + candidateAdviceMethod);
		}

		AbstractAspectJAdvice springAdvice;

		//AspectJAnnotation中会存放通知类型
		switch (aspectJAnnotation.getAnnotationType()) {
		    ////如果是@PointCut方法，则什么也不做
			case AtPointcut:
				if (logger.isDebugEnabled()) {
					logger.debug("Processing pointcut '" + candidateAdviceMethod.getName() + "'");
				}
				return null;
            //如果是环绕通知，则直接创建AspectJAroundAdvice实例
            //入参为：通知方法、切点表达式、切面实例
			case AtAround:
				springAdvice = new AspectJAroundAdvice(
						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
				break;
            //如果是前置通知，则直接创建AspectJMethodBeforeAdvice实例
            //入参为：通知方法、切点表达式、切面实例
			case AtBefore:
				springAdvice = new AspectJMethodBeforeAdvice(
						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
				break;
            //如果是后置通知，则直接创建AspectJAfterAdvice实例
            //入参为:通知方法、切点表达式、切面实例
			case AtAfter:
				springAdvice = new AspectJAfterAdvice(
						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
				break;
            //如果是后置返回通知，则直接创建AspectJAfterReturningAdvice实例
            //入参为：通知方法、切点表达式、切面实例
			case AtAfterReturning:
				springAdvice = new AspectJAfterReturningAdvice(
						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
                //设置后置返回值的参数name
				AfterReturning afterReturningAnnotation = (AfterReturning) aspectJAnnotation.getAnnotation();
				if (StringUtils.hasText(afterReturningAnnotation.returning())) {
					springAdvice.setReturningName(afterReturningAnnotation.returning());
				}
				break;
            //如果是后置异常通知，则直接创建AspectJAfterThrowingAdvice实例
            //入参为：通知方法、切点表达式、切面实例
			case AtAfterThrowing:
				springAdvice = new AspectJAfterThrowingAdvice(
						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
				AfterThrowing afterThrowingAnnotation = (AfterThrowing) aspectJAnnotation.getAnnotation();
				if (StringUtils.hasText(afterThrowingAnnotation.throwing())) {
					springAdvice.setThrowingName(afterThrowingAnnotation.throwing());
				}
				break;
            //上面的那几种情况都不是的话，则抛出异常
			default:
				throw new UnsupportedOperationException(
						"Unsupported advice type on method: " + candidateAdviceMethod);
		}

		// Now to configure the advice...
        // 设置切面的名字
		springAdvice.setAspectName(aspectName);
		springAdvice.setDeclarationOrder(declarationOrder);
        //横切方法的注解中的参数名
		String[] argNames = this.parameterNameDiscoverer.getParameterNames(candidateAdviceMethod);
		if (argNames != null) {
			springAdvice.setArgumentNamesFromStringArray(argNames);
		}
        //参数绑定，参照这个方法的分析
		springAdvice.calculateArgumentBindings();

		return springAdvice;
	}


	/**
	 * Synthetic advisor that instantiates the aspect.
	 * Triggered by per-clause pointcut on non-singleton aspect.
	 * The advice has no effect.
	 */
	@SuppressWarnings("serial")
	protected static class SyntheticInstantiationAdvisor extends DefaultPointcutAdvisor {

		public SyntheticInstantiationAdvisor(final MetadataAwareAspectInstanceFactory aif) {
			super(aif.getAspectMetadata().getPerClausePointcut(), (MethodBeforeAdvice)
					(method, args, target) -> aif.getAspectInstance());
		}
	}

}

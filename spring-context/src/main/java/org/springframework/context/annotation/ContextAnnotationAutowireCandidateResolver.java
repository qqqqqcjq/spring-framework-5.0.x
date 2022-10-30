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

package org.springframework.context.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 继承自QualifierAnnotationAutowireCandidateResolver，增加对isLazy=true时的懒处理(注意此处懒处理(延迟处理)，不是懒加载)
 * @Lazy一般含义是懒加载，对@Lazy的处理是BeanDefinition.setLazyInit(true) ==>  isLazy=true  (register bd的时候处理)
 * 这个类就是处理isLazy=true的情况
 *
 * Complete implementation of the
 * {@link org.springframework.beans.factory.support.AutowireCandidateResolver} strategy
 * interface, providing support for qualifier annotations as well as for lazy resolution
 * driven by the {@link Lazy} annotation in the {@code context.annotation} package.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */

// @since 4.0 出现得挺晚，它支持到了@Lazy  是功能最全的AutowireCandidateResolver
public class ContextAnnotationAutowireCandidateResolver extends QualifierAnnotationAutowireCandidateResolver {

	@Override
	@Nullable
    // 这是此类本身唯一做的事，此处精析
    // 返回该 lazy proxy 表示延迟初始化，实现过程是查看在 @Autowired 注解处是否使用了 @Lazy = true 注解
	public Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, @Nullable String beanName) {

        // 如果isLazy=true  那就返回一个代理，否则返回null
        // 相当于若标注了@Lazy注解，就会返回一个代理（当然@Lazy注解的value值不能是false, 默认就是true，不用修改即可）
		return (isLazy(descriptor) ? buildLazyResolutionProxy(descriptor, beanName) : null);
	}

    // 这个比较简单，@Lazy注解标注了就行（value属性默认值是true）
    // @Lazy支持注解 属性  方法入参 ... @Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.FIELD})
    // 这里都会解析
	protected boolean isLazy(DependencyDescriptor descriptor) {
		for (Annotation ann : descriptor.getAnnotations()) {
			Lazy lazy = AnnotationUtils.getAnnotation(ann, Lazy.class);
			if (lazy != null && lazy.value()) {
				return true;
			}
		}
		MethodParameter methodParam = descriptor.getMethodParameter();
		if (methodParam != null) {
			Method method = methodParam.getMethod();
			if (method == null || void.class == method.getReturnType()) {
				Lazy lazy = AnnotationUtils.getAnnotation(methodParam.getAnnotatedElement(), Lazy.class);
                return lazy != null && lazy.value();
			}
		}
		return false;
	}

    // 核心内容，是本类的灵魂
	protected Object buildLazyResolutionProxy(final DependencyDescriptor descriptor, final @Nullable String beanName) {
		Assert.state(getBeanFactory() instanceof DefaultListableBeanFactory,
				"BeanFactory needs to be a DefaultListableBeanFactory");

        // 这里毫不客气的直接使用了DefaultListableBeanFactory，使用了DefaultListableBeanFactory.doResolveDependency()方法
		final DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) getBeanFactory();

        // TargetSource 是它实现懒加载的核心原因，在AOP那一章节了重点提到过这个接口，此处不再叙述
        // 它有很多的著名实现如HotSwappableTargetSource、SingletonTargetSource、LazyInitTargetSource、
        // SimpleBeanTargetSource、ThreadLocalTargetSource、PrototypeTargetSource等等非常多
        // 此处因为只需要自己用，所以采用匿名内部类的方式实现  此处最重要是看getTarget方法，它在被使用的时候（也就是代理对象真正使用的时候执行~~~）
		TargetSource ts = new TargetSource() {
			@Override
			public Class<?> getTargetClass() {
				return descriptor.getDependencyType();
			}
			@Override
			public boolean isStatic() {
				return false;
			}

			@Override
            // getTarget是调用代理方法的时候会调用的，所以执行每个代理方法都会执行此方法
			public Object getTarget() {
				Object target = beanFactory.doResolveDependency(descriptor, beanName, null, null);
				if (target == null) {
					Class<?> type = getTargetClass();
					if (Map.class == type) {
						return Collections.emptyMap();
					}
					else if (List.class == type) {
						return Collections.emptyList();
					}
					else if (Set.class == type || Collection.class == type) {
						return Collections.emptySet();
					}
					throw new NoSuchBeanDefinitionException(descriptor.getResolvableType(),
							"Optional dependency not present for lazy injection point");
				}
				return target;
			}
			@Override
			public void releaseTarget(Object target) {
			}
		};

        // 使用ProxyFactory  给ts生成一个代理
        // 由此可见最终生成的代理对象的目标对象其实是TargetSource,而TargetSource的目标才是我们业务的对象
		ProxyFactory pf = new ProxyFactory();
		pf.setTargetSource(ts);
		Class<?> dependencyType = descriptor.getDependencyType();

		//Class中的native boolean isInterface()方法 ： 确定指定的{@code Class}对象是否是接口。
		if (dependencyType.isInterface()) {
			pf.addInterface(dependencyType);
		}
		return pf.getProxy(beanFactory.getBeanClassLoader());
	}

}


/*
后记：
它很好的用到了TargetSource这个接口，结合动态代理来支持到了@Lazy注解。
标注有@Lazy注解完成注入的时候，最终注入只是一个此处临时生成的代理对象，只有在真正执行目标方法的时候才会去容器内拿到真是的bean实例来执行目标方法。

通过@Lazy注解能够解决很多情况下的循环依赖问题，它的基本思想是先'随便'给你创建一个代理对象先放着，等你真正执行方法的时候再实际去容器内找出目标实例执行~
我们要明白这种解决问题的思路带来的好处是能够解决很多场景下的循环依赖问题，但是要知道它每次执行目标方法的时候都会去执行TargetSource.getTarget()方法，
所以需要做好缓存，避免对执行效率的影响（实测执行效率上的影响可以忽略不计）
@Component
public class CircularDependencyA {

    private CircularDependencyB circB;

    @Autowired
    public CircularDependencyA(CircularDependencyB circB) {
        this.circB = circB;
    }
}
@Component
public class CircularDependencyB {

    private CircularDependencyA circA;

    @Autowired
    public CircularDependencyB(CircularDependencyA circA) {
        this.circA = circA;
    }
}
上面会因为循环依赖报错
可以用@Lazy解决
@Component
public class CircularDependencyA {

    private CircularDependencyB circB;

    @Autowired
    public CircularDependencyA(@Lazy CircularDependencyB circB) {
        this.circB = circB;
    }
}

ContextAnnotationAutowireCandidateResolver这个处理器才是被Bean工厂最终实际使用的，因为它的功能是最全的(很多功能在父类实现)
 */
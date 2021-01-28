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

package org.springframework.beans.factory.support;

import java.lang.reflect.Method;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Basic {@link AutowireCandidateResolver} that performs a full generic type
 * match with the candidate's type if the dependency is declared as a generic type
 * (e.g. Repository&lt;Customer&gt;).
 *
 * <p>This is the base class for
 * {@link org.springframework.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver},
 * providing an implementation all non-annotation-based resolution steps at this level.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
// 从名字可以看出和泛型有关。Spring4.0后的泛型依赖注入主要是它来实现的，所以
// spring4.0之后兼容了泛型依赖注入，所以在依赖注入流程里面，这个类的相关流程方法必须要执行一遍，这个类中的流程也是spring4.0之后加入依赖注入的注流程中的。
// 可以看出，spring 增加流程的方式很优雅，通过实现接口进行扩展，而不是直接在源代码中增加if else这种巨low的方式
public class GenericTypeAwareAutowireCandidateResolver extends SimpleAutowireCandidateResolver
		implements BeanFactoryAware {

	@Nullable
	private BeanFactory beanFactory;


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Nullable
	protected final BeanFactory getBeanFactory() {
		return this.beanFactory;
	}


	@Override
    // 是否允许被依赖
    // 因为bean定义里默认是true，绝大多数情况下我们不会修改它
    // 所以继续执行：checkGenericTypeMatch 看看泛型类型是否能够匹配上
    // 若能够匹配上   这个就会被当作候选的Bean了
	public boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
        // 如果bean定义里面已经不允许了  那就不往下走了  显然我们不会这么做
		if (!super.isAutowireCandidate(bdHolder, descriptor)) {
			// If explicitly false, do not proceed with any other checks...
			return false;
		}

        // 处理泛型依赖的核心方法,也是本实现类的灵魂
        // 注意：这里还兼容到了工厂方法配置bean，参见RootBeanDefinition.factoryMethodReturnType
		return checkGenericTypeMatch(bdHolder, descriptor);
	}

	/**
	 * Match the given dependency type with its generic type information agaPersistenceAnnotationProcessorinst the given
	 * candidate bean definition.
	 */
	protected boolean checkGenericTypeMatch(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
		ResolvableType dependencyType = descriptor.getResolvableType();
		if (dependencyType.getType() instanceof Class) {
			// No generic type -> we know it's a Class type-match, so no need to check again.
			return true;
		}

		ResolvableType targetType = null;
		boolean cacheType = false;
		RootBeanDefinition rbd = null;
		if (bdHolder.getBeanDefinition() instanceof RootBeanDefinition) {
			rbd = (RootBeanDefinition) bdHolder.getBeanDefinition();
		}
		if (rbd != null) {
			targetType = rbd.targetType;
			if (targetType == null) {
				cacheType = true;
				// First, check factory method return type, if applicable
				targetType = getReturnTypeForFactoryMethod(rbd, descriptor);
				if (targetType == null) {
					RootBeanDefinition dbd = getResolvedDecoratedDefinition(rbd);
					if (dbd != null) {
						targetType = dbd.targetType;
						if (targetType == null) {
							targetType = getReturnTypeForFactoryMethod(dbd, descriptor);
						}
					}
				}
			}
		}

		if (targetType == null) {
			// Regular case: straight bean instance, with BeanFactory available.
			if (this.beanFactory != null) {
				Class<?> beanType = this.beanFactory.getType(bdHolder.getBeanName());
				if (beanType != null) {
					targetType = ResolvableType.forClass(ClassUtils.getUserClass(beanType));
				}
			}
			// Fallback: no BeanFactory set, or no type resolvable through it
			// -> best-effort match against the target class if applicable.
			if (targetType == null && rbd != null && rbd.hasBeanClass() && rbd.getFactoryMethodName() == null) {
				Class<?> beanClass = rbd.getBeanClass();
				if (!FactoryBean.class.isAssignableFrom(beanClass)) {
					targetType = ResolvableType.forClass(ClassUtils.getUserClass(beanClass));
				}
			}
		}

		if (targetType == null) {
			return true;
		}
		if (cacheType) {
			rbd.targetType = targetType;
		}
		if (descriptor.fallbackMatchAllowed() && targetType.hasUnresolvableGenerics()) {
			return true;
		}
		// Full check for complex generic type match...
		return dependencyType.isAssignableFrom(targetType);
	}

	@Nullable
	protected RootBeanDefinition getResolvedDecoratedDefinition(RootBeanDefinition rbd) {
		BeanDefinitionHolder decDef = rbd.getDecoratedDefinition();
		if (decDef != null && this.beanFactory instanceof ConfigurableListableBeanFactory) {
			ConfigurableListableBeanFactory clbf = (ConfigurableListableBeanFactory) this.beanFactory;
			if (clbf.containsBeanDefinition(decDef.getBeanName())) {
				BeanDefinition dbd = clbf.getMergedBeanDefinition(decDef.getBeanName());
				if (dbd instanceof RootBeanDefinition) {
					return (RootBeanDefinition) dbd;
				}
			}
		}
		return null;
	}

	@Nullable
	protected ResolvableType getReturnTypeForFactoryMethod(RootBeanDefinition rbd, DependencyDescriptor descriptor) {
		// Should typically be set for any kind of factory method, since the BeanFactory
		// pre-resolves them before reaching out to the AutowireCandidateResolver...
		ResolvableType returnType = rbd.factoryMethodReturnType;
		if (returnType == null) {
			Method factoryMethod = rbd.getResolvedFactoryMethod();
			if (factoryMethod != null) {
				returnType = ResolvableType.forMethodReturnType(factoryMethod);
			}
		}
		if (returnType != null) {
			Class<?> resolvedClass = returnType.resolve();
			if (resolvedClass != null && descriptor.getDependencyType().isAssignableFrom(resolvedClass)) {
				// Only use factory method metadata if the return type is actually expressive enough
				// for our dependency. Otherwise, the returned instance type may have matched instead
				// in case of a singleton instance having been registered with the container already.
				return returnType;
			}
		}
		return null;
	}

}

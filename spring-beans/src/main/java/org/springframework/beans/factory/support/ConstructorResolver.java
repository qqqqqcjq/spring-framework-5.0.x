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

package org.springframework.beans.factory.support;

import java.beans.ConstructorProperties;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MethodInvoker;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Delegate for resolving constructors and factory methods.
 * Performs constructor resolution through argument matching.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Mark Fisher
 * @author Costin Leau
 * @author Sebastien Deleuze
 * @since 2.0
 * @see #autowireConstructor
 * @see #instantiateUsingFactoryMethod
 * @see AbstractAutowireCapableBeanFactory
 */
//根据参数等条件匹配合适的构造函数/工厂方法，然后委托构造函数/工厂方法来创建实例
class ConstructorResolver {

	private static final NamedThreadLocal<InjectionPoint> currentInjectionPoint =
			new NamedThreadLocal<>("Current injection point");

	private final AbstractAutowireCapableBeanFactory beanFactory;

	private final Log logger;


	/**
	 * Create a new ConstructorResolver for the given factory and instantiation strategy.
	 * @param beanFactory the BeanFactory to work with
	 */
	public ConstructorResolver(AbstractAutowireCapableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		this.logger = beanFactory.getLogger();
	}


	/**
	 * "autowire constructor" (with constructor arguments by type) behavior.
	 * Also applied if explicit constructor argument values are specified,
	 * matching all remaining arguments with beans from the bean factory.
	 * <p>This corresponds to constructor injection: In this mode, a Spring
	 * bean factory is able to host components that expect constructor-based
	 * dependency resolution.
	 * @param beanName the name of the bean
	 * @param mbd the merged bean definition for the bean
	 * @param chosenCtors chosen candidate constructors (or {@code null} if none)
	 * @param explicitArgs argument values passed in programmatically via the getBean method,
	 * or {@code null} if none (-> use constructor argument values from bean definition)
	 * @return a BeanWrapper for the new instance
	 */
	//使用选择的构造函数构造和自动装配bean,如果指定了显式构造函数参数值，那么会应用它，也会使用来自bean工厂的bean匹配所有剩余的参数。
	//beanName ： beanName
    //mbd : 要构造的bean的merge bean definition
    //chosenCtors : 选择的用来构造bean的构造函数
    //explicitArgs ： 构造函数需要的参数数组Object[]，就从bean definition中查找参数值(xml中配置的 或者 手工给bd设置的)， 没有的话通过spring容器getBean方法得到的
	public BeanWrapper autowireConstructor(String beanName, RootBeanDefinition mbd,
			@Nullable Constructor<?>[] chosenCtors, @Nullable Object[] explicitArgs) {

		//实例一个BeanWrapperImpl 对象很好理解
		//前面外部返回的BeanWrapper 其实就是这个BeanWrapperImpl
		//因为BeanWrapper是个接口
		BeanWrapperImpl bw = new BeanWrapperImpl();
		this.beanFactory.initBeanWrapper(bw);

		Constructor<?> constructorToUse = null;
		ArgumentsHolder argsHolderToUse = null;

		//最终实例化使用的，保存构造方法需要的参数值
		Object[] argsToUse = null;

		// 正常通过getBean()->createBean()->流程走到这里，explicitArgs这个参数传的是null
		if (explicitArgs != null) {
			argsToUse = explicitArgs;
		}
		else {
		    //================= 一个bean实例化过程中第一次走到这里这些值都是没有的，后面的过程解析完成获取这些值时才会缓存到bd里面  begin=====================
			Object[] argsToResolve = null;
			synchronized (mbd.constructorArgumentLock) {
				//获取已解析的构造方法
				//一般不会有，因为构造方法一般会提供一个
				//除非有多个。那么才会存在已经解析完成的构造方法
				constructorToUse = (Constructor<?>) mbd.resolvedConstructorOrFactoryMethod;
				if (constructorToUse != null && mbd.constructorArgumentsResolved) {
					// Found a cached constructor...
					argsToUse = mbd.resolvedConstructorArguments;
					if (argsToUse == null) {
						argsToResolve = mbd.preparedConstructorArguments;
					}
				}
			}
			if (argsToResolve != null) {
				argsToUse = resolvePreparedArguments(beanName, mbd, bw, constructorToUse, argsToResolve);
			}
            //================= 一个bean实例化过程中第一次走到这里这些值都是没有的，后面的过程解析完成获取这些值时才会缓存到bd里面  end=====================
		}

        //第一次实例化，没有缓存，constructorToUse为null, 进入下面的解析，流程
		if (constructorToUse == null) {
			//如果没有已经解析的构造方法， 则需要去解析构造方法
			// Need to resolve the constructor.
			// 构造函数不为空或者AbstractBeanDefinition.autowireMode是AUTOWIRE_CONSTRUCTOR，就表示需要构造函数自动注入
			boolean autowiring = (chosenCtors != null || mbd.getResolvedAutowireMode() == AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR);
			ConstructorArgumentValues resolvedValues = null;

			//定义了最小参数个数
			//如果你给构造方法的参数列表给定了具体的值
			//那么这些值得个数就是构造方法参数的个数
            int minNrOfArgs;
			if (explicitArgs != null) {
				minNrOfArgs = explicitArgs.length;
			}
			else {
                /**
			    *  例子1 ：
                *	模拟mybatis中到dao实例的构造  gbd.getConstructorArgumentValues().addGenericArgumentValue("simulate.mapperscan.CardDao");
                *	public class MyImpotBeanDefinitionRgister implements ImportBeanDefinitionRegistrar {
                *		public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
                *
                *		  //因为CardDao是一个接口，所以这样得到的bd,在实例化的时候会报错, 所以我们还需要借助FactoryBean和JDK动态代理
                *		  //BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(CardDao.class);
                *		  //GenericBeanDefinition gbd = (GenericBeanDefinition) beanDefinitionBuilder.getBeanDefinition();
                *		  //beanDefinitionRegistry.registerBeanDefinition("cardDao",gbd);
                *
                *			BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(MyFactoryBean.class);
                *			GenericBeanDefinition gbd = (GenericBeanDefinition) beanDefinitionBuilder.getBeanDefinition();
                *			//gbd对应的类没有默认构造方法了，下面的代码可以让spring通过有参数的构造方法实例化bean
                *			gbd.getConstructorArgumentValues().addGenericArgumentValue("simulate.mapperscan.CardDao");
                *			beanDefinitionRegistry.registerBeanDefinition("cardDao",gbd);
                *		}
                *	}
                */
                /**
                 * 例子2：
                 * 确定构造方法参数数量,假设有如下配置：
                 *     <bean id="luban" class="com.luban.Luban">
                 *         <constructor-arg index="0" value="str1"/>
                 *         <constructor-arg index="1" value="1"/>
                 *         <constructor-arg index="2" value="str2"/>
                 *     </bean>
                 *
                 * 在通过spring内部给了一个值得情况那么表示你的构造方法的最小参数个数一定
                 * minNrOfArgs = 3
                 */
			    //======================先尝试从mbd中获取构造函数的参数值，放进ConstructorArgumentValues resolvedValues begin===================================
				//实例一个对象，用来存放构造方法的参数值
				//当中主要存放了参数值和参数值所对应的下标
				ConstructorArgumentValues cargs = mbd.getConstructorArgumentValues();
				resolvedValues = new ConstructorArgumentValues();
				minNrOfArgs = resolveConstructorArguments(beanName, mbd, bw, cargs, resolvedValues);
                //======================先尝试从mbd中获取构造函数的参数值，放进ConstructorArgumentValues resolvedValues end===================================
			}

			//========================获取构造函数  begin ===================================
			// Take specified constructors, if any.
			Constructor<?>[] candidates = chosenCtors;
			if (candidates == null) {
				Class<?> beanClass = mbd.getBeanClass();
				try {
					candidates = (mbd.isNonPublicAccessAllowed() ?
							beanClass.getDeclaredConstructors() : beanClass.getConstructors());
				}
				catch (Throwable ex) {
					throw new BeanCreationException(mbd.getResourceDescription(), beanName,
							"Resolution of declared constructors on bean Class [" + beanClass.getName() +
							"] from ClassLoader [" + beanClass.getClassLoader() + "] failed", ex);
				}
			}

            //根据构造方法的访问权限级别和参数数量进行排序
            //怎么排序的呢？
            /**
             *  根据访问权限，参数个数进行排序
             *  这个自己可以写个测试去看看到底是不是和我说的一样
             * 1. public Luban(Object o1, Object o2, Object o3)
             * 2. public Luban(Object o1, Object o2)
             * 3. public Luban(Object o1)
             * 4. protected Luban(Integer i, Object o1, Object o2, Object o3)
             * 5. protected Luban(Integer i, Object o1, Object o2)
             * 6. protected Luban(Integer i, Object o1)
             */
            //为什么要进行排序呢？
            //spring 按照访问权限 参数个数进行排序，然后遍历候选构造函数，这样匹配到合适的构造函数后就可以break了
            /**
             * if (constructorToUse != null && argsToUse.length > paramTypes.length) {
             *     // Already found greedy constructor that can be satisfied ->
             *     // do not look any further, there are only less greedy constructors left.
             *     break;
             * }
             */
            AutowireUtils.sortConstructors(candidates);
            //========================获取构造函数并排序  end ====================================



			//定义了一个差异变量，这个变量很有分量，后面有注释
            //直接根据这个单词也可以看出意思 ： 最小的类型差异权重，也就是候选构造函数的参数类型列表 和 解析出来的参数的类型 之间进行比较，差异最小(关系最近的父子类型)的候选构造函数，就是我们最终要使用的构造函数
			int minTypeDiffWeight = Integer.MAX_VALUE;
			//ambiguous ： 模糊不清的，模棱两可的；不明确的，不明朗的；引起歧义的
            //spring根据差异量来确认构造函数，找到多个的话会先保存在ambiguousConstructors
			Set<Constructor<?>> ambiguousConstructors = null;
			LinkedList<UnsatisfiedDependencyException> causes = null;

			//=========循环所有的构造方法, 找到一个合适的构造函数constructorToUse以及需要用到的参数值argsToUse   begin==============================
			for (Constructor<?> candidate : candidates) {
				Class<?>[] paramTypes = candidate.getParameterTypes();
				/**
				 * 这个判断别看只有一行代码理解起来很费劲
				 * 首先constructorToUse != null这个很好理解，
				 * 前面已经说过首先constructorToUse主要是用来装已经解析过了并且在使用的构造方法
				 * 只有在他等于空的情况下，才有继续的意义，因为下面如果解析到了一个符合的构造方法
				 * 就会赋值给这个变量（下面注释有写）。故而如果这个变量不等于null就不需要再进行解析了，说明spring已经
				 * 找到一个合适的构造方法，直接使用便可以
				 * argsToUse.length > paramTypes.length这个代码就相当复杂了
				 * 首先假设 argsToUse = [1,"luban"，obj]
				 * 那么回去匹配到上面的构造方法的1和5
				 * 由于构造方法1有更高的访问权限，所有选择1，尽管5看起来更加匹配
				 * 但是我们看2,直接参数个数就不对所以直接忽略
				 */
				if (constructorToUse != null && argsToUse.length > paramTypes.length) {
					// Already found greedy constructor that can be satisfied ->
					// do not look any further, there are only less greedy constructors left.
					break;
				}
				if (paramTypes.length < minNrOfArgs) {
					continue;
				}

				ArgumentsHolder argsHolder;
				if (resolvedValues != null) {
					try {
						//判断是否加了ConstructorProperties注解如果加了则把值取出来
						//可以写个代码测试一下
						//@ConstructorProperties(value = {"xxx", "111"})
						String[] paramNames = ConstructorPropertiesChecker.evaluate(candidate, paramTypes.length);
						if (paramNames == null) {
							ParameterNameDiscoverer pnd = this.beanFactory.getParameterNameDiscoverer();
							if (pnd != null) {
								//获取构造方法参数名称列表
								/**
								 * 假设你有一个（String luban,Object zilu）
								 * 则paramNames=[luban,zilu]
								 */
								paramNames = pnd.getParameterNames(candidate);
							}
						}

						//获取构造方法参数值列表，根据参数名字列表paramTypes，获取对应的参数值
						argsHolder = createArgumentArray(beanName, mbd, resolvedValues, bw, paramTypes, paramNames,
								getUserDeclaredConstructor(candidate), autowiring);
					}
					catch (UnsatisfiedDependencyException ex) {
						if (logger.isTraceEnabled()) {
							logger.trace("Ignoring constructor [" + candidate + "] of bean '" + beanName + "': " + ex);
						}
						// Swallow and try next constructor.
						if (causes == null) {
							causes = new LinkedList<>();
						}
						causes.add(ex);
						continue;
					}
				}
				else {
					// Explicit arguments given -> arguments length must match exactly.
					if (paramTypes.length != explicitArgs.length) {
						continue;
					}
					argsHolder = new ArgumentsHolder(explicitArgs);
				}

				/**
				 * typeDiffWeight 差异量，何谓差异量呢？
				 * argsHolder.arguments和paramTypes之间的差异
				 * 每个参数值的类型与构造方法参数列表的类型直接的差异
				 * 通过这个差异量来衡量或者确定一个合适的构造方法
				 *
				 * 值得注意的是constructorToUse=candidate
				 *
				 * 第一次循环一定会typeDiffWeight < minTypeDiffWeight，因为minTypeDiffWeight的值非常大
				 * 然后每次循环会把typeDiffWeight赋值给minTypeDiffWeight（minTypeDiffWeight = typeDiffWeight）
				 * else if (constructorToUse != null && typeDiffWeight == minTypeDiffWeight)
				 * 第一次循环肯定不会进入这个
				 * 第二次如果进入了这个分支代表什么？
				 * 代表有两个构造方法都符合我们要求？那么spring又迷茫了（spring经常在迷茫）
				 * spring迷茫了怎么办？ 先把他们保存在ambiguousConstructors.add(candidate)(ambiguousConstructors顾名思义)
				 * ambiguousConstructors=null 非常重要？
				 * 为什么重要，因为需要清空
				 * 这也解释了为什么他找到两个符合要求的方法不直接抛异常的原因，因为继续循环可能找到他们更加符合的构造方法，这样spring 就不用迷茫了，用这个更加符合的构造方法，清空ambiguousConstructors
				 * 如果这个ambiguousConstructors一直存在，spring会在循环外面去exception
				 * 很牛逼呀！！！！
				 */
				//程佳清注释：  使用宽松模式和严格模式都可以
                //             宽松模式根据参数实例对应的类和候选方法参数类型之间的继承关系计算差异权重
                //             严格模式根据参数实例是否是候选方法参数类型(调用isAssignableFrom进行判断)就按差异权重
				int typeDiffWeight = (mbd.isLenientConstructorResolution() ?
						argsHolder.getTypeDifferenceWeight(paramTypes) : argsHolder.getAssignabilityWeight(paramTypes));
				// Choose this constructor if it represents the closest match.
				if (typeDiffWeight < minTypeDiffWeight) {
					constructorToUse = candidate;
					argsHolderToUse = argsHolder;
					argsToUse = argsHolder.arguments;
					minTypeDiffWeight = typeDiffWeight;
					ambiguousConstructors = null;
				}
				else if (constructorToUse != null && typeDiffWeight == minTypeDiffWeight) {
					if (ambiguousConstructors == null) {
						ambiguousConstructors = new LinkedHashSet<>();
						ambiguousConstructors.add(constructorToUse);
					}
					ambiguousConstructors.add(candidate);
				}
			}
			//=========循环所有的构造方法, 找到一个合适的构造函数constructorToUse以及需要用到的参数值argsToUse   end===============================

            //======================如果没有匹配到合适的构造函数，或者ambiguousConstructors没有被上面置为null, 就抛出异常结束本方法  begin====================
			//没有找打合适的构造方法
			if (constructorToUse == null) {
				if (causes != null) {
					UnsatisfiedDependencyException ex = causes.removeLast();
					for (Exception cause : causes) {
						this.beanFactory.onSuppressedException(cause);
					}
					throw ex;
				}
				throw new BeanCreationException(mbd.getResourceDescription(), beanName,
						"Could not resolve matching constructor " +
						"(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities)");
			}

			//如果ambiguousConstructors还存在则异常？为什么会在上面方法中直接exception？
			//上面注释当中有说明
			else if (ambiguousConstructors != null && !mbd.isLenientConstructorResolution()) {
				throw new BeanCreationException(mbd.getResourceDescription(), beanName,
						"Ambiguous constructor matches found in bean '" + beanName + "' " +
						"(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities): " +
						ambiguousConstructors);
			}
            //======================如果没有匹配到合适的构造函数，或者ambiguousConstructors没有被上面置为null, 就抛出异常结束本方法  end=====================

            //======把参数信息 构造方法信息等缓存到mbd中，以方便下次实例化  begin==============
            // explicitArgs意思是已经完全明确的参数列表
            // 正常通过getBean()->createBean()->流程走到这里，explicitArgs这个参数传的是null
			if (explicitArgs == null) {
				/*
				 * 缓存相关信息，比如：
				 *   1. 已解析出的构造方法对象 resolvedConstructorOrFactoryMethod
				 *   2. 构造方法参数列表是否已解析标志 constructorArgumentsResolved
				 *   3. 参数值列表 resolvedConstructorArguments 或 preparedConstructorArguments
				 *   这些信息可用在其他地方，用于进行快捷判断
				 */
				argsHolderToUse.storeCache(mbd, constructorToUse);
			}
            //======把参数信息 构造方法信息等缓存到mbd中，以方便下次实例化  end==============
		}

		//=======================创建实例   begin==================================
		try {
			/*
			 * 使用反射创建实例 lookup-method 通过CGLIB增强bean实例
			 */
			final InstantiationStrategy strategy = beanFactory.getInstantiationStrategy();
			Object beanInstance;

			if (System.getSecurityManager() != null) {
				final Constructor<?> ctorToUse = constructorToUse;
				final Object[] argumentsToUse = argsToUse;
				beanInstance = AccessController.doPrivileged((PrivilegedAction<Object>) () ->
						strategy.instantiate(mbd, beanName, beanFactory, ctorToUse, argumentsToUse),
						beanFactory.getAccessControlContext());
			}
			else {
				beanInstance = strategy.instantiate(mbd, beanName, this.beanFactory, constructorToUse, argsToUse);
			}

			bw.setBeanInstance(beanInstance);
			return bw;
		}
		catch (Throwable ex) {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName,
					"Bean instantiation via constructor failed", ex);
		}
        //=======================创建实例   end==================================
	}

	/**
	 * Resolve the factory method in the specified bean definition, if possible.
	 * {@link RootBeanDefinition#getResolvedFactoryMethod()} can be checked for the result.
	 * @param mbd the bean definition to check
	 */
	//如果可能的话，在指定的bean定义中解析工厂方法。包括静态工厂方法和实例工厂方法
	public void resolveFactoryMethodIfPossible(RootBeanDefinition mbd) {
		//定义factoryClass用于保存工厂类的类对象
	    Class<?> factoryClass;
	    //定义是否是静态标记
		boolean isStatic;
		//如果mbd的FactoryBean名不为null(注意和FactoryBean功能没有关系，这里的factoryBean是指要调用factory method的实例，如果是静态工厂方法，则为{@code null})
		if (mbd.getFactoryBeanName() != null) {
            //使用beanFactory确定mbd的FactoryBean名的bean类型。为了确定其对象类型，默认让FactoryBean以初始化
			factoryClass = this.beanFactory.getType(mbd.getFactoryBeanName());
            //静态标记设置为false，表示不是静态方法
			isStatic = false;
		}
		else {
            //获取mbd包装好的Bean类
			factoryClass = mbd.getBeanClass();
            //静态标记设置为true，表示是静态方法
			isStatic = true;
		}
        //如果factoryClass为null,抛出异常：无法解析工厂类
		Assert.state(factoryClass != null, "Unresolvable factory class");
        //如果factoryClass是CGLIB生成的子类，则返回该子类的父类，否则直接返回factoryClass
		factoryClass = ClassUtils.getUserClass(factoryClass);
        //根据mbd的是否允许访问非公共构造函数和方法标记【RootBeanDefinition.isNonPublicAccessAllowed】来获取factoryClass的所有候选方法
		Method[] candidates = getCandidateMethods(factoryClass, mbd);
        //定义用于存储唯一方法对象的Method对象
		Method uniqueCandidate = null;
        //遍历candidates
		for (Method candidate : candidates) {
            //如果candidate的静态标记与静态标记相同 且 candidate有资格作为工厂方法
			if (Modifier.isStatic(candidate.getModifiers()) == isStatic && mbd.isFactoryMethod(candidate)) {
                //如果uniqueCandidate还没有引用
			    if (uniqueCandidate == null) {
                    //将uniqueCandidate引用该candidate
					uniqueCandidate = candidate;
				}
                //如果uniqueCandidate的参数类型数组与candidate的参数类型数组不一致
				else if (!Arrays.equals(uniqueCandidate.getParameterTypes(), candidate.getParameterTypes())) {
                    //取消uniqueCandidate的引用
				    uniqueCandidate = null;
                    //跳出循环
					break;
				}
			}
		}
        //将mbd用于自省的唯一工厂方法候选的缓存引用上uniqueCandidate
		synchronized (mbd.constructorArgumentLock) {
			mbd.resolvedConstructorOrFactoryMethod = uniqueCandidate;
		}
	}

	/**
	 * Retrieve all candidate methods for the given class, considering
	 * the {@link RootBeanDefinition#isNonPublicAccessAllowed()} flag.
	 * Called as the starting point for factory method determination.
	 */
	//检索
	private Method[] getCandidateMethods(Class<?> factoryClass, RootBeanDefinition mbd) {
	    // 如果有系统安全管理器
		if (System.getSecurityManager() != null) {
            // 使用特权方式执行:如果mbd允许访问非公共构造函数和方法，就返回factoryClass子类和其父类的所有声明方法，首先包括子类方法；
            // 否则只获取factoryClass的public级别方法
			return AccessController.doPrivileged((PrivilegedAction<Method[]>) () ->
					(mbd.isNonPublicAccessAllowed() ?
						ReflectionUtils.getAllDeclaredMethods(factoryClass) : factoryClass.getMethods()));
		}
		else {
            // 如果mbd允许访问非公共构造函数和方法，就返回factoryClass子类和其父类的所有声明方法，首先包括子类方法；
            // 否则只获取factoryClass的public级别方法
			return (mbd.isNonPublicAccessAllowed() ?
					ReflectionUtils.getAllDeclaredMethods(factoryClass) : factoryClass.getMethods());
		}
	}

	/**
	 * Instantiate the bean using a named factory method. The method may be static, if the
	 * bean definition parameter specifies a class, rather than a "factory-bean", or
	 * an instance variable on a factory object itself configured using Dependency Injection.
	 * <p>Implementation requires iterating over the static or instance methods with the
	 * name specified in the RootBeanDefinition (the method may be overloaded) and trying
	 * to match with the parameters. We don't have the types attached to constructor args,
	 * so trial and error is the only way to go here. The explicitArgs array may contain
	 * argument values passed in programmatically via the corresponding getBean method.
	 * @param beanName the name of the bean
	 * @param mbd the merged bean definition for the bean
	 * @param explicitArgs argument values passed in programmatically via the getBean  通过getBean方法传下来的参数值, 如果为空的话，使用bd中保存的构造函数的参数
	 * method, or {@code null} if none (-> use constructor argument values from bean definition)
	 * @return a BeanWrapper for the new instance
	 */

    //具体查看调用链就明白了
    //工厂方法是指： 传统的静态工厂方法， 传统的动态工厂方法， 静态@bean method, @bean method, 这些都是工厂方法

    // instantiateUsingFactoryMethod方法体很大，但是其核心点就是确定 工厂方法/@bean method(这2种会调用这个方法进行实例化，具体查看调用链就明白了)，获取参数，
    // 最后通过CglibSubclassingInstantiationStrategy#instantiate反射执行工厂方法创建bean对象。
    // @param beanName the name of the bean : beanname
    // @param mbd the merged bean definition for the bean ： mbd(@bean method引入的bean对应的bd是ConfigurationClassBeanDefinitionReader$ConfigurationClassBeanDefinition类型)
    // @param explicitArgs ： 通过getBean方法传下来的参数值, 如果为空的话，使用bd中保存的构造函数的参数
	public BeanWrapper instantiateUsingFactoryMethod(
			String beanName, RootBeanDefinition mbd, @Nullable Object[] explicitArgs) {
        // 构造BeanWrapperImpl对象
		BeanWrapperImpl bw = new BeanWrapperImpl();
        // 初始化BeanWrapperImpl 向BeanWrapper对象中添加ConversionService对象和属性编辑器PropertyEditor对象
		this.beanFactory.initBeanWrapper(bw);

        // 跟FactoryBean没有关系，这里的factoryBean是指要调用factory method的实例，
        // 如果是静态工厂方法或者是静态@bean method作为工厂方法，则为{@code null}
        // 如果是动态工厂方法，则为声明这个动态工厂方法的类的实例
        // 如果是@bean method作为工厂方法，则为声明这个@bean method的类的实例
        Object factoryBean;
        //Object factoryBean对应的class
		Class<?> factoryClass;
		//是否是静态工厂方法或者静态@bean method作为工厂方法
		boolean isStatic;

		String factoryBeanName = mbd.getFactoryBeanName();
		if (factoryBeanName != null) {
			if (factoryBeanName.equals(beanName)) {
				throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName,
						"factory-bean reference points back to the same bean definition");
			}
			factoryBean = this.beanFactory.getBean(factoryBeanName);
			if (mbd.isSingleton() && this.beanFactory.containsSingleton(beanName)) {
				throw new ImplicitlyAppearedSingletonException();
			}
			factoryClass = factoryBean.getClass();
			isStatic = false;
		}
		else {
			// It's a static factory method on the bean class.
			if (!mbd.hasBeanClass()) {
				throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName,
						"bean definition declares neither a bean class nor a factory-bean reference");
			}
			factoryBean = null;
			factoryClass = mbd.getBeanClass();
			isStatic = true;
		}

        // 获得factoryMethodToUse、argsHolderToUse、argsToUse属性， factoryMethodToUse argsToUse就是最终要使用的工厂方法和参数值
		Method factoryMethodToUse = null;
		ArgumentsHolder argsHolderToUse = null;
		Object[] argsToUse = null;


        // 正常通过getBean()->createBean()->流程走到这里，explicitArgs这个参数传的是null
		if (explicitArgs != null) {
		    //使用getBean method传进来的参数
			argsToUse = explicitArgs;
		}
		else {
            //================= 一个bean实例化过程中第一次走到这里这些值都是没有的，后面的过程解析完成获取这些值时才会缓存到bd里面  begin=====================
			Object[] argsToResolve = null;
			synchronized (mbd.constructorArgumentLock) {
				factoryMethodToUse = (Method) mbd.resolvedConstructorOrFactoryMethod;
				if (factoryMethodToUse != null && mbd.constructorArgumentsResolved) {
					// Found a cached factory method...
					argsToUse = mbd.resolvedConstructorArguments;
					if (argsToUse == null) {
						argsToResolve = mbd.preparedConstructorArguments;
					}
				}
			}
			if (argsToResolve != null) {
				argsToUse = resolvePreparedArguments(beanName, mbd, bw, factoryMethodToUse, argsToResolve);
			}
            //================= 一个bean实例化过程中第一次走到这里这些值都是没有的，后面的过程解析完成获取这些值时才会缓存到bd里面  begin=====================
		}

		if (factoryMethodToUse == null || argsToUse == null) {
			// Need to determine the factory method...
			// Try all methods with this name to see if they match the given arguments.
			factoryClass = ClassUtils.getUserClass(factoryClass);

			Method[] rawCandidates = getCandidateMethods(factoryClass, mbd);
			List<Method> candidateList = new ArrayList<>();
			for (Method candidate : rawCandidates) {
				if (Modifier.isStatic(candidate.getModifiers()) == isStatic && mbd.isFactoryMethod(candidate)) {
					candidateList.add(candidate);
				}
			}
			Method[] candidates = candidateList.toArray(new Method[0]);
			AutowireUtils.sortFactoryMethods(candidates);

			//不要被这个名字给骗了，工厂方法需要的参数和构造函数需要的参数都是封装在ConstructorArgumentValues 变量里面
			ConstructorArgumentValues resolvedValues = null;
			boolean autowiring = (mbd.getResolvedAutowireMode() == AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR);
			int minTypeDiffWeight = Integer.MAX_VALUE;
			Set<Method> ambiguousFactoryMethods = null;

			int minNrOfArgs;
			if (explicitArgs != null) {
				minNrOfArgs = explicitArgs.length;
			}
			else {
				// We don't have arguments passed in programmatically, so we need to resolve the
				// arguments specified in the constructor arguments held in the bean definition.
				if (mbd.hasConstructorArgumentValues()) {
					ConstructorArgumentValues cargs = mbd.getConstructorArgumentValues();
					resolvedValues = new ConstructorArgumentValues();
                    //======================= 解析工厂方法需要的参数 封装在ConstructorArgumentValues resolvedValues 里面 begin ========================================================
                    //解析工厂方法需要的参数，封装在ConstructorArgumentValues resolvedValues 里面
					minNrOfArgs = resolveConstructorArguments(beanName, mbd, bw, cargs, resolvedValues);
                    //======================= 解析工厂方法需要的参数 封装在ConstructorArgumentValues resolvedValues 里面 end ========================================================
                }
				else {
					minNrOfArgs = 0;
				}
			}

			LinkedList<UnsatisfiedDependencyException> causes = null;

			for (Method candidate : candidates) {
			    //候选方法的参数类型列表
				Class<?>[] paramTypes = candidate.getParameterTypes();

				//只有paramTypes.length >= minNrOfArgs才会进入，可见，如果paramTypes.length小于minNrOfArgs，那么这个候选方法肯定不对
				if (paramTypes.length >= minNrOfArgs) {
					ArgumentsHolder argsHolder;

					if (explicitArgs != null) {
						// Explicit arguments given -> arguments length must match exactly.
						if (paramTypes.length != explicitArgs.length) {
							continue;
						}
						argsHolder = new ArgumentsHolder(explicitArgs);
					}
					else {
						// Resolved constructor arguments: type conversion and/or autowiring necessary.
						try {
							String[] paramNames = null;
							ParameterNameDiscoverer pnd = this.beanFactory.getParameterNameDiscoverer();
							if (pnd != null) {
								paramNames = pnd.getParameterNames(candidate);
							}
							//根据候选方法的参数类型列表paramTypes和已经解析完成的参数resolvedValues，得到最终的参数保存在argsHolder
							argsHolder = createArgumentArray(
									beanName, mbd, resolvedValues, bw, paramTypes, paramNames, candidate, autowiring);
						}
						catch (UnsatisfiedDependencyException ex) {
							if (logger.isTraceEnabled()) {
								logger.trace("Ignoring factory method [" + candidate + "] of bean '" + beanName + "': " + ex);
							}
							// Swallow and try next overloaded factory method.
							if (causes == null) {
								causes = new LinkedList<>();
							}
							causes.add(ex);
							continue;
						}
					}

                    //程佳清注释：  使用宽松模式和严格模式都可以
                    //             宽松模式根据参数实例对应的类和候选方法参数类型之间的继承关系计算差异权重
                    //             严格模式根据参数实例是否是候选方法参数类型(调用isAssignableFrom进行判断)就按差异权重
					int typeDiffWeight = (mbd.isLenientConstructorResolution() ?
							argsHolder.getTypeDifferenceWeight(paramTypes) : argsHolder.getAssignabilityWeight(paramTypes));
					// Choose this factory method if it represents the closest match.
					if (typeDiffWeight < minTypeDiffWeight) {
						factoryMethodToUse = candidate;
						argsHolderToUse = argsHolder;
						argsToUse = argsHolder.arguments;
						minTypeDiffWeight = typeDiffWeight;
						ambiguousFactoryMethods = null;
					}
					// Find out about ambiguity: In case of the same type difference weight
					// for methods with the same number of parameters, collect such candidates
					// and eventually raise an ambiguity exception.
					// However, only perform that check in non-lenient constructor resolution mode,
					// and explicitly ignore overridden methods (with the same parameter signature).
					else if (factoryMethodToUse != null && typeDiffWeight == minTypeDiffWeight &&
							!mbd.isLenientConstructorResolution() &&
							paramTypes.length == factoryMethodToUse.getParameterCount() &&
							!Arrays.equals(paramTypes, factoryMethodToUse.getParameterTypes())) {
						if (ambiguousFactoryMethods == null) {
							ambiguousFactoryMethods = new LinkedHashSet<>();
							ambiguousFactoryMethods.add(factoryMethodToUse);
						}
						ambiguousFactoryMethods.add(candidate);
					}
				}
			}

			if (factoryMethodToUse == null) {
				if (causes != null) {
					UnsatisfiedDependencyException ex = causes.removeLast();
					for (Exception cause : causes) {
						this.beanFactory.onSuppressedException(cause);
					}
					throw ex;
				}
				List<String> argTypes = new ArrayList<>(minNrOfArgs);
				if (explicitArgs != null) {
					for (Object arg : explicitArgs) {
						argTypes.add(arg != null ? arg.getClass().getSimpleName() : "null");
					}
				}
				else if (resolvedValues != null) {
					Set<ValueHolder> valueHolders = new LinkedHashSet<>(resolvedValues.getArgumentCount());
					valueHolders.addAll(resolvedValues.getIndexedArgumentValues().values());
					valueHolders.addAll(resolvedValues.getGenericArgumentValues());
					for (ValueHolder value : valueHolders) {
						String argType = (value.getType() != null ? ClassUtils.getShortName(value.getType()) :
								(value.getValue() != null ? value.getValue().getClass().getSimpleName() : "null"));
						argTypes.add(argType);
					}
				}
				String argDesc = StringUtils.collectionToCommaDelimitedString(argTypes);
				throw new BeanCreationException(mbd.getResourceDescription(), beanName,
						"No matching factory method found: " +
						(mbd.getFactoryBeanName() != null ?
							"factory bean '" + mbd.getFactoryBeanName() + "'; " : "") +
						"factory method '" + mbd.getFactoryMethodName() + "(" + argDesc + ")'. " +
						"Check that a method with the specified name " +
						(minNrOfArgs > 0 ? "and arguments " : "") +
						"exists and that it is " +
						(isStatic ? "static" : "non-static") + ".");
			}
			else if (void.class == factoryMethodToUse.getReturnType()) {
				throw new BeanCreationException(mbd.getResourceDescription(), beanName,
						"Invalid factory method '" + mbd.getFactoryMethodName() +
						"': needs to have a non-void return type!");
			}
			else if (ambiguousFactoryMethods != null) {
				throw new BeanCreationException(mbd.getResourceDescription(), beanName,
						"Ambiguous factory method matches found in bean '" + beanName + "' " +
						"(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities): " +
						ambiguousFactoryMethods);
			}

			if (explicitArgs == null && argsHolderToUse != null) {
				argsHolderToUse.storeCache(mbd, factoryMethodToUse);
			}
		}

		try {
			Object beanInstance;

            /**
             * //@param factoryBean  这里的factoryBean是指：
             * //1. factory method 实例化：factoryBean是指要调用factory method的实例，如果是静态工厂方法，则为{@code null}
             * //2. @bean method作为工厂方法实例化： @Configuration注解的类的代理类实例化后的bean，如果是静态@bean method作为工厂方法，则为{@code null}
             * //@param factoryMethod 这里的factoryMethod是指：
             * //1. factory method 实例化：配置的factory method
             * //2. @bean method实例化： @bean 注解的方法
             * 	Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
             *                        @Nullable Object factoryBean, Method factoryMethod, @Nullable Object... args)
             * 			throws BeansException;
             */
			if (System.getSecurityManager() != null) {
				final Object fb = factoryBean;
				final Method factoryMethod = factoryMethodToUse;
				final Object[] args = argsToUse;
				beanInstance = AccessController.doPrivileged((PrivilegedAction<Object>) () ->
						beanFactory.getInstantiationStrategy().instantiate(mbd, beanName, beanFactory, fb, factoryMethod, args),
						beanFactory.getAccessControlContext());
			}
			else {
				beanInstance = this.beanFactory.getInstantiationStrategy().instantiate(
						mbd, beanName, this.beanFactory, factoryBean, factoryMethodToUse, argsToUse);
			}

			bw.setBeanInstance(beanInstance);
			return bw;
		}
		catch (Throwable ex) {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName,
					"Bean instantiation via factory method failed", ex);
		}
	}

	/**
	 * Resolve the constructor arguments for this bean into the resolvedValues object.
	 * This may involve looking up other beans.
	 * <p>This method is also used for handling invocations of static factory methods.
	 */
	//构造函数实例化(autowireConstructor() -> resolveConstructorArguments())或者工厂方法实例化(instantiateUsingFactoryMethod() -> resolveConstructorArguments())的时候都会调用这个方法
	//将这个bean的构造函数参数解析为ConstructorArgumentValues resolvedValues对象。这可能涉及到查找其他bean。
    //这个方法解析的构造函数参数可能来自下下面2种方式
    /**
     *  例子1 ：
     *	模拟mybatis中到dao实例的构造  gbd.getConstructorArgumentValues().addGenericArgumentValue("simulate.mapperscan.CardDao");
     *	public class MyImpotBeanDefinitionRgister implements ImportBeanDefinitionRegistrar {
     *		public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
     *
     *		  //因为CardDao是一个接口，所以这样得到的bd,在实例化的时候会报错, 所以我们还需要借助FactoryBean和JDK动态代理
     *		  //BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(CardDao.class);
     *		  //GenericBeanDefinition gbd = (GenericBeanDefinition) beanDefinitionBuilder.getBeanDefinition();
     *		  //beanDefinitionRegistry.registerBeanDefinition("cardDao",gbd);
     *
     *			BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(MyFactoryBean.class);
     *			GenericBeanDefinition gbd = (GenericBeanDefinition) beanDefinitionBuilder.getBeanDefinition();
     *			//gbd对应的类没有默认构造方法了，下面的代码可以让spring通过有参数的构造方法实例化bean
     *			gbd.getConstructorArgumentValues().addGenericArgumentValue("simulate.mapperscan.CardDao");
     *			beanDefinitionRegistry.registerBeanDefinition("cardDao",gbd);
     *		}
     *	}
     */
    /**
     * 例子2：
     * 确定构造方法参数数量,假设有如下配置：
     *     <bean id="luban" class="com.luban.Luban">
     *         <constructor-arg index="0" value="str1"/>
     *         <constructor-arg index="1" value="1"/>
     *         <constructor-arg index="2" value="str2"/>
     *     </bean>
     */
    // @param cargs ： 这个是从bd中直接获取然后作为参数传给这个方法的，也就是说，实例化之前，bd中就已经保存了AbstractBeanDefinition.constructorArgumentValues。
    //          比如上面的例子1和例子2在实例化之前就已经保存在AbstractBeanDefinition.constructorArgumentValues中
    // @param resolvedValues ： 遍历cargs，完成类型转换，并且计算出需要的最小参数个数
    // @return minNrOfArgs : 返回构造方法或者工厂方法需要的最小参数个数
	private int resolveConstructorArguments(String beanName, RootBeanDefinition mbd, BeanWrapper bw,
			ConstructorArgumentValues cargs, ConstructorArgumentValues resolvedValues) {

	    //TypeConverter可以使用PropertyEditor或者ConversionService完成类型转换
		TypeConverter customConverter = this.beanFactory.getCustomTypeConverter();
		TypeConverter converter = (customConverter != null ? customConverter : bw);
		BeanDefinitionValueResolver valueResolver =
				new BeanDefinitionValueResolver(this.beanFactory, beanName, mbd, converter);

		int minNrOfArgs = cargs.getArgumentCount();

		for (Map.Entry<Integer, ConstructorArgumentValues.ValueHolder> entry : cargs.getIndexedArgumentValues().entrySet()) {
			int index = entry.getKey();
			if (index < 0) {
				throw new BeanCreationException(mbd.getResourceDescription(), beanName,
						"Invalid constructor argument index: " + index);
			}
			if (index > minNrOfArgs) {
				minNrOfArgs = index + 1;
			}
			ConstructorArgumentValues.ValueHolder valueHolder = entry.getValue();
			if (valueHolder.isConverted()) {
				resolvedValues.addIndexedArgumentValue(index, valueHolder);
			}
			else {
				Object resolvedValue =
						valueResolver.resolveValueIfNecessary("constructor argument", valueHolder.getValue());
				ConstructorArgumentValues.ValueHolder resolvedValueHolder =
						new ConstructorArgumentValues.ValueHolder(resolvedValue, valueHolder.getType(), valueHolder.getName());
				resolvedValueHolder.setSource(valueHolder);
				resolvedValues.addIndexedArgumentValue(index, resolvedValueHolder);
			}
		}

		for (ConstructorArgumentValues.ValueHolder valueHolder : cargs.getGenericArgumentValues()) {
		    //参数已经完成转换，直接添加到resolvedValues
			if (valueHolder.isConverted()) {
				resolvedValues.addGenericArgumentValue(valueHolder);
			}
            //参数没有经过转换，调用BeanDefinitionValueResolver.resolveValueIfNecessary()完成转换，然后添加到resolvedValues
			else {
				Object resolvedValue =
						valueResolver.resolveValueIfNecessary("constructor argument", valueHolder.getValue());
				ConstructorArgumentValues.ValueHolder resolvedValueHolder = new ConstructorArgumentValues.ValueHolder(
						resolvedValue, valueHolder.getType(), valueHolder.getName());
				resolvedValueHolder.setSource(valueHolder);
				resolvedValues.addGenericArgumentValue(resolvedValueHolder);
			}
		}

		return minNrOfArgs;
	}

	// 这个方法相当重要，会返回一个数据，里面保存了实例化bean的方法可能需要的参数
    // 实例化bean的方法包括：构造方法(autowireConstructor()->createArgumentArray()) 或者 工厂方法(静态工厂方法，动态工厂方法, 静态@bean method作为工厂方法，@bean method作为工厂方法，instantiateUsingFactoryMethod() -> createArgumentArray())
	// 解析过程包括：
    // 从bean definition中查找参数值(xml中配置的 或者 手工给bd设置的)获取， 没有的话通过spring容器getBean方法得到的
    /**
     * Create an array of arguments to invoke a constructor or factory method,
     * given the resolved constructor argument values.
     * @param beanName bean名
     * @param mbd beanName对于的合并后RootBeanDefinition
     * @param resolvedValues 已经过解析的构造函数参数值Holder对象
     * @param bw bean实例包装类
     * @param paramTypes executable的参数类型数组
     * @param paramNames  executable的参数名数组
     * @param executable 候选方法
     * @param autowiring mbd是否支持使用构造函数进行自动注入的标记
     * @param fallback 是否可在抛出NoSuchBeanDefinitionException返回null，而不抛出异常
     */
	private ArgumentsHolder createArgumentArray(
			String beanName, RootBeanDefinition mbd, @Nullable ConstructorArgumentValues resolvedValues,
			BeanWrapper bw, Class<?>[] paramTypes, @Nullable String[] paramNames, Executable executable,
			boolean autowiring) throws UnsatisfiedDependencyException {

        //获取bean工厂的自定义的TypeConverter
		TypeConverter customConverter = this.beanFactory.getCustomTypeConverter();
        //如果customeConverter不为null,converter就引用customeConverter，否则引用bw
		TypeConverter converter = (customConverter != null ? customConverter : bw);
        //根据paramTypes的数组长度构建一个ArgumentsHolder实例，用于保存解析后的参数值
		ArgumentsHolder args = new ArgumentsHolder(paramTypes.length);
        //定义一个用于存储构造函数参数 值 的ValueHolder列表，以查找下一个任意泛型参数值时，忽略该集合的元素的HashSet,初始化长度为paramTypes的数组长度
		Set<ConstructorArgumentValues.ValueHolder> usedValueHolders = new HashSet<>(paramTypes.length);
        //定义一个用于存储需要自动注入的参数的beanname
		Set<String> autowiredBeanNames = new LinkedHashSet<>(4);

        //fori形式遍历paramType,索引为paramIndex
		for (int paramIndex = 0; paramIndex < paramTypes.length; paramIndex++) {
            //获取paramTypes中第paramIndex个参数类型
			Class<?> paramType = paramTypes[paramIndex];
            //如果paramNames不为null，就引用第paramIndex个参数名否则引用空字符串
			String paramName = (paramNames != null ? paramNames[paramIndex] : "");


			// Try to find matching constructor argument value, either indexed or generic.
            // 定义一个用于存储与paramIndex对应的ConstructorArgumentValues.ValueHolder实例
			ConstructorArgumentValues.ValueHolder valueHolder = null;

            //====================先尝试从传入的resolvedValues参数中找到匹配的构造函数参数值,放入valueHolder  begin=====================
			if (resolvedValues != null) {
			    //通过下标获取参数值
				valueHolder = resolvedValues.getArgumentValue(paramIndex, paramType, paramName, usedValueHolders);
				// If we couldn't find a direct match and are not supposed to autowire,
				// let's try the next generic, untyped argument value as fallback:
				// it could match after type conversion (for example, String -> int).
                // 如果通过下标找不到直接匹配并且不希望自动装配，请尝试使用一个通用的，无类型的参数值作为后备：
                // 类型转换后可以匹配(例如String -> int)

                // 如果valueHolder为null 且 (mbd不支持使用构造函数进行自动注入 或者 paramTypes数组长度与resolvedValues的(下标索引参数值+泛型参数值)数量相等)
				if (valueHolder == null && (!autowiring || paramTypes.length == resolvedValues.getArgumentCount())) {
					valueHolder = resolvedValues.getGenericArgumentValue(null, null, usedValueHolders);
				}
			}
            //====================先尝试从传入的resolvedValues参数中找到匹配的构造函数参数值,放入valueHolder  end======================

            //====================我们从resolvedValues找到了一个可能的匹配对象，让我们试一试 begin==================================
			if (valueHolder != null) {
				// We found a potential match - let's give it a try.  我们找到了一个可能的匹配对象，让我们试一试。
				// Do not consider the same value definition multiple times!  不要多次考虑同一个值的定义!

                //将valueHolder添加到usedValueHolders中，以表示该valueHolder已经使用过，下次在resolvedValues中获取下一个valueHolder时，不要返回同一个对象
                usedValueHolders.add(valueHolder);
                //从valueHolder中获取原始参数值
				Object originalValue = valueHolder.getValue();
                //定义一个用于存储转换后的参数值的Object对象
				Object convertedValue;
                //如果valueHolder已经包含转换后的值
				if (valueHolder.isConverted()) {
                    //从valueHolder中获取转换后的参数值
					convertedValue = valueHolder.getConvertedValue();
                    //将convertedValue保存到args的preparedArguments数组的paramIndex对应元素中
					args.preparedArguments[paramIndex] = convertedValue;
				}
				else {
                    //将executable中paramIndex对应的参数封装成MethodParameter对象
					MethodParameter methodParam = MethodParameter.forExecutable(executable, paramIndex);
					try {
                        //使用converter将originalValue转换为paramType类型
						convertedValue = converter.convertIfNecessary(originalValue, paramType, methodParam);
					}
                    //捕捉在转换类型时出现的类型不匹配异常
					catch (TypeMismatchException ex) {
                        //重新抛出不满足的依赖异常：无法将类型[valueHolder.getValue（）的类名]的参数值转换为所需的类型[paramType.getName（）]
						throw new UnsatisfiedDependencyException(
								mbd.getResourceDescription(), beanName, new InjectionPoint(methodParam),
								"Could not convert argument value of type [" +
										ObjectUtils.nullSafeClassName(valueHolder.getValue()) +
										"] to required type [" + paramType.getName() + "]: " + ex.getMessage());
					}
                    //获取valueHolder的源对象，一般是ValueHolder
					Object sourceHolder = valueHolder.getSource();
                    //如果sourceHolder是ConstructorArgumentValues.ValueHolder实例
					if (sourceHolder instanceof ConstructorArgumentValues.ValueHolder) {
                        //将soureHolder转换为ConstructorArgumentValues.ValueHolder对象
						Object sourceValue = ((ConstructorArgumentValues.ValueHolder) sourceHolder).getValue();
                        //将args的resolveNecessary该为true，表示args.preparedArguments需要解析
						args.resolveNecessary = true;
                        //将sourceValue保存到args的preparedArguments数组的paramIndex对应元素中
						args.preparedArguments[paramIndex] = sourceValue;
					}
				}
                //将convertedValue保存到args的arguments数组的paramIndex对应元素中
				args.arguments[paramIndex] = convertedValue;
                //将convertedValue保存到args的arguments数组的paramIndex对应元素中
				args.rawArguments[paramIndex] = originalValue;
			}
            //====================我们从resolvedValues找到了一个可能的匹配对象，让我们试一试   end====================================

            //====================我们没有从resolvedValues找到匹配对象，走下面的分支 ，getBean()获取参数值 begin=============================================
			else {
                //将executable中paramIndex对应的参数封装成MethodParameter对象
				MethodParameter methodParam = MethodParameter.forExecutable(executable, paramIndex);
				// No explicit match found: we're either supposed to autowire or
				// have to fail creating an argument array for the given constructor.
                // 找不到明确的匹配项:我们要么自动装配，自动装配失败的话就只能抛出异常
                // mbd不支持适用构造函数进行自动注入
                if (!autowiring) {
					throw new UnsatisfiedDependencyException(
							mbd.getResourceDescription(), beanName, new InjectionPoint(methodParam),
							"Ambiguous argument values for parameter of type [" + paramType.getName() +
							"] - did you specify the correct bean references as arguments?");
				}

				try {
				    //！！！！！！！！！！！！！！！！！！！！！！重要的方法：通过这个resolveAutowiredArgument()这个方法获取构造参数的参数值！！！！！！！！！！！！！！！！！！！！！！！
                    //！！！！！！！！！！！！！！！！！！！！！！会调用this.beanFactory.resolveDependency()方法获取依赖值！！！！！！！！！！！！！！！！！！！！！！
					Object autowiredArgument =  resolveAutowiredArgument(methodParam, beanName, autowiredBeanNames, converter); //需要自动注入的bean的beanname都会保存在autowiredBeanNames中

                    //将autowiredArgument保存到args的rawArguments数组的paramIndex对应元素中
                    args.rawArguments[paramIndex] = autowiredArgument;
                    //将autowiredArgument保存到args的arguments数组的paramIndex对应元素中
					args.arguments[paramIndex] = autowiredArgument;
                    //标记这个参数是通过自动装配获取的
					args.preparedArguments[paramIndex] = new AutowiredArgumentMarker();
                    //将args的resolveNecessary该为true，表示args.preparedArguments需要解析
					args.resolveNecessary = true;
				}
				catch (BeansException ex) {
					throw new UnsatisfiedDependencyException(
							mbd.getResourceDescription(), beanName, new InjectionPoint(methodParam), ex);
				}
			}
            //====================我们没有从resolvedValues找到匹配对象，走上面的分支  end=============================================
		}

		//============保存依赖关系，记录依赖关系的日志 begin====================================
		for (String autowiredBeanName : autowiredBeanNames) {
			this.beanFactory.registerDependentBean(autowiredBeanName, beanName);
			if (logger.isDebugEnabled()) {
				logger.debug("Autowiring by type from bean name '" + beanName +
						"' via " + (executable instanceof Constructor ? "constructor" : "factory method") +
						" to bean named '" + autowiredBeanName + "'");
			}
		}
        //============保存依赖关系，记录依赖关系的日志 end======================================

		return args;
	}

	/**
	 * Resolve the prepared arguments stored in the given bean definition.
	 */
	// ConstructorResolver.ArgumentsHolder.storeCache()会保存相关参数信息, 第二次实例化的时候就可以用这些缓存信息，调用这个方法解析 prepared arguments准备的参数
	private Object[] resolvePreparedArguments(
			String beanName, RootBeanDefinition mbd, BeanWrapper bw, Executable executable, Object[] argsToResolve) {

	    //获取bean工厂的自定义的TypeConverter
		TypeConverter customConverter = this.beanFactory.getCustomTypeConverter();
		//如果customeConverter不为null,converter就引用customeConverter，否则引用bw
		TypeConverter converter = (customConverter != null ? customConverter : bw);
        //BeanDefinitionValueResolver主要是用于将bean定义对象中包含的值解析为应用于目标bean实例的实际值
        //新建一个BeanDefinitionValueResolver解析器对象
		BeanDefinitionValueResolver valueResolver =
				new BeanDefinitionValueResolver(this.beanFactory, beanName, mbd, converter);
        //从executable中获取其参数类型
		Class<?>[] paramTypes = executable.getParameterTypes();

        //定义一个解析后的参数值数组,长度argsToResolve的长度
		Object[] resolvedArgs = new Object[argsToResolve.length];
        //遍历argsToResolve(fori形式)
		for (int argIndex = 0; argIndex < argsToResolve.length; argIndex++) {
            //获取argsToResolver的第argIndex个参数值
			Object argValue = argsToResolve[argIndex];
            //为executable的argIndex位置参数创建一个新的MethodParameter对象
			MethodParameter methodParam = MethodParameter.forExecutable(executable, argIndex);
			//确定给定泛型参数类型的目标类型
			GenericTypeResolver.resolveParameterType(methodParam, executable.getDeclaringClass());
            //如果agrValue是自动装配的参数标记，使用this.beanFactory.resolveDependency()获取需要依赖注入的值
			if (argValue instanceof AutowiredArgumentMarker) {
				argValue = resolveAutowiredArgument(methodParam, beanName, null, converter);
			}
            //BeanMetadataElement:由包含配置源对象的bean元数据元素实现的接口,BeanDefinition的父接口
            //如果argValue是BeanMetadataElement对象，使用BeanDefinitionValueResolver解析器进行解析，具体参考BeanDefinitionValueResolver.resolveValueIfNecessary()
			else if (argValue instanceof BeanMetadataElement) {
				argValue = valueResolver.resolveValueIfNecessary("constructor argument", argValue);
			}
			//如果argValue是String类型就使用表达式解析器进行解析
			else if (argValue instanceof String) {
				argValue = this.beanFactory.evaluateBeanDefinitionString((String) argValue, mbd);
			}

			//  ================根据参数下标得到要求的类型，然后将解析后的argValue转换为要求的类型  begin=================
			Class<?> paramType = paramTypes[argIndex];
			try {
				resolvedArgs[argIndex] = converter.convertIfNecessary(argValue, paramType, methodParam);
			}
			catch (TypeMismatchException ex) {
				throw new UnsatisfiedDependencyException(
						mbd.getResourceDescription(), beanName, new InjectionPoint(methodParam),
						"Could not convert argument value of type [" + ObjectUtils.nullSafeClassName(argValue) +
						"] to required type [" + paramType.getName() + "]: " + ex.getMessage());
			}
            //  ================根据参数下标得到要求的类型，然后将解析后的argValue转换为要求的类型  end===================
		}
		return resolvedArgs;
	}

	protected Constructor<?> getUserDeclaredConstructor(Constructor<?> constructor) {
		Class<?> declaringClass = constructor.getDeclaringClass();
		Class<?> userClass = ClassUtils.getUserClass(declaringClass);
		if (userClass != declaringClass) {
			try {
				return userClass.getDeclaredConstructor(constructor.getParameterTypes());
			}
			catch (NoSuchMethodException ex) {
				// No equivalent constructor on user class (superclass)...
				// Let's proceed with the given constructor as we usually would.
			}
		}
		return constructor;
	}

	/**
	 * Template method for resolving the specified argument which is supposed to be autowired.
	 */
	@Nullable
    //获取自动注入的参数的参数值
    //this.beanFactory.resolveDependency
	protected Object resolveAutowiredArgument(MethodParameter param, String beanName,
			@Nullable Set<String> autowiredBeanNames, TypeConverter typeConverter) {

		if (InjectionPoint.class.isAssignableFrom(param.getParameterType())) {
			InjectionPoint injectionPoint = currentInjectionPoint.get();
			if (injectionPoint == null) {
				throw new IllegalStateException("No current InjectionPoint available for " + param);
			}
			return injectionPoint;
		}
		return this.beanFactory.resolveDependency(
				new DependencyDescriptor(param, true), beanName, autowiredBeanNames, typeConverter);
	}

	static InjectionPoint setCurrentInjectionPoint(@Nullable InjectionPoint injectionPoint) {
		InjectionPoint old = currentInjectionPoint.get();
		if (injectionPoint != null) {
			currentInjectionPoint.set(injectionPoint);
		}
		else {
			currentInjectionPoint.remove();
		}
		return old;
	}


	/**
	 * Private inner class for holding argument combinations.
	 */
	//私有内部类，用于保存参数组合
	private static class ArgumentsHolder {

	    // 原始参数值数组
		public final Object[] rawArguments;

		//经过转换后参数值数组
		public final Object[] arguments;

		//准备好的参数值数组，保存着 源参数值和AutowiredArgumentMarker实例
		public final Object[] preparedArguments;

		//resolveNecessary这个是标识参数需要进行解析，存入preparedArguments还是arguments的标识
		public boolean resolveNecessary = false;

		public ArgumentsHolder(int size) {
			this.rawArguments = new Object[size];
			this.arguments = new Object[size];
			this.preparedArguments = new Object[size];
		}

		public ArgumentsHolder(Object[] args) {
			this.rawArguments = args;
			this.arguments = args;
			this.preparedArguments = args;
		}

        /**
         * 获取类型差异权重，宽容模式下使用
         * <ol>
         *  <li>获取表示paramTypes和arguments之间的类层次结构差异的权重【变量 typeDiffWeight】</li>
         *  <li>获取表示paramTypes和rawArguments之间的类层次结构差异的权重【变量 rawTypeDiffWeight】</li>
         *  <li>比较typeDiffWeight和rawTypeDiffWeight取最小权重并返回出去，但是还是以原始类型优先，因为差异值还-1024</li>
         * </ol>
         * @param paramTypes 参数类型数组
         * @return 类型差异权重最小值
         */

		public int getTypeDifferenceWeight(Class<?>[] paramTypes) {
			// If valid arguments found, determine type difference weight.
			// Try type difference weight on both the converted arguments and
			// the raw arguments. If the raw weight is better, use it.
			// Decrease raw weight by 1024 to prefer it over equal converted weight.
            //获取paramTypes和arguments之间类型差异的权重
			int typeDiffWeight = MethodInvoker.getTypeDifferenceWeight(paramTypes, this.arguments);
            //获取paramTypes和rawArguments之间类型差异的权重
			int rawTypeDiffWeight = MethodInvoker.getTypeDifferenceWeight(paramTypes, this.rawArguments) - 1024;
			return (rawTypeDiffWeight < typeDiffWeight ? rawTypeDiffWeight : typeDiffWeight);
		}

        /**
         * 获取Assignabliity权重，严格模式下使用
         * <ol>
         *  <li>fori形式遍历paramTypes:
         *   <ol>
         *    <li>如果确定arguments不是paramTypes的实例,返回Integer最大值;意味着既然连最终的转换后参数值都不能匹配，这个情况下
         *    paramTypes所对应的工厂方法是不可以接受的</li>
         *   </ol>
         *  </li>
         *  <li>fori形式遍历paramTypes:
         *   <ol>
         *    <li>如果确定rawArguments不是paramTypes的实例,返回Integer最大值-512;意味着虽然转换后的参数值匹配，但是原始的参数值不匹配，
         *    这个情况下的paramTypes所对应的工厂方法还是可以接受的</li>
         *   </ol>
         *  </li>
         *  <li>在完全匹配的情况下，返回Integer最大值-1024；意味着因为最终的转换后参数值和原始参数值都匹配，
         *  这种情况下paramTypes所对应的工厂方法非常可以接收</li>
         * </ol>
         * <p>补充：为啥这里使用Integer.MAX_VALUE作为最初比较值呢？我猜测是因为业务比较时采用谁小谁优先原则。至于为啥-512，和-1024呢？这个我也没懂，但
         * 至少-512，-1024所得到结果比起-1，-2的结果会明显很多。</p>
         * @param paramTypes 参数类型
         * @return Assignabliity权重
         */

		public int getAssignabilityWeight(Class<?>[] paramTypes) {
			for (int i = 0; i < paramTypes.length; i++) {
                //主要判断this.arguments[i]是否是paramTypes[i]的子类或者同类
				if (!ClassUtils.isAssignableValue(paramTypes[i], this.arguments[i])) {
					return Integer.MAX_VALUE;
				}
			}
			for (int i = 0; i < paramTypes.length; i++) {
			    //主要判断this.rawArguments[i]是否是paramTypes[i]的子类或者同类
				if (!ClassUtils.isAssignableValue(paramTypes[i], this.rawArguments[i])) {
					return Integer.MAX_VALUE - 512;
				}
			}
			return Integer.MAX_VALUE - 1024;
		}

		public void storeCache(RootBeanDefinition mbd, Executable constructorOrFactoryMethod) {
			synchronized (mbd.constructorArgumentLock) {
				mbd.resolvedConstructorOrFactoryMethod = constructorOrFactoryMethod;
				mbd.constructorArgumentsResolved = true;
				if (this.resolveNecessary) {
					mbd.preparedConstructorArguments = this.preparedArguments;
				}
				else {
					mbd.resolvedConstructorArguments = this.arguments;
				}
			}
		}
	}


	/**
	 * Marker for autowired arguments in a cached argument array.
 	 */
	//标记一个参数是需要自动注入的
	private static class AutowiredArgumentMarker {
	}


	/**
	 * Delegate for checking Java 6's {@link ConstructorProperties} annotation.
	 */
	//用于检查Java 6的{@link ConstructorProperties}注解的委托类
	private static class ConstructorPropertiesChecker {

        /**
         * 获取candidate的ConstructorProperties注解的name属性值
         * <ol>
         *  <li>获取candidated中的ConstructorProperties注解 【变量 cp】</li>
         *  <li>如果cp不为null:
         *   <ol>
         *    <li>获取cp指定的getter方法的属性名 【变量 names】</li>
         *    <li>如果names长度于paramCount不相等,抛出IllegalStateException</li>
         *    <li>将name返回出去</li>
         *   </ol>
         *  </li>
         *  <li>如果没有配置ConstructorProperties注解，则返回null</li>
         * </ol>
         * @param candidate 候选方法
         * @param paramCount candidate的参数梳理
         * @return candidate的ConstructorProperties注解的name属性值
         */
		@Nullable
		public static String[] evaluate(Constructor<?> candidate, int paramCount) {
		    //获取candidated中的ConstructorProperties注解
			ConstructorProperties cp = candidate.getAnnotation(ConstructorProperties.class);
            //如果cp不为null
			if (cp != null) {
                //获取cp指定的getter方法的属性名
				String[] names = cp.value();
                //如果names长度于paramCount不相等
				if (names.length != paramCount) {
				    //抛出IllegalStateException:用@ConstructorPropertie注解的构造方法，不对应实际的参数数量(paramCount):candidate
					throw new IllegalStateException("Constructor annotated with @ConstructorProperties but not " +
							"corresponding to actual number of parameters (" + paramCount + "): " + candidate);
				}
                //将name返回出去
				return names;
			}
			else {
                //如果没有配置ConstructorProperties注解，则返回null
				return null;
			}
		}
	}

}

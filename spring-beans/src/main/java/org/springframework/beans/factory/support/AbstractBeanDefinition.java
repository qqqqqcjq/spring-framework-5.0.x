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

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.beans.BeanMetadataAttributeAccessor;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Base class for concrete, full-fledged {@link BeanDefinition} classes,
 * factoring out common properties of {@link GenericBeanDefinition},
 * {@link RootBeanDefinition}, and {@link ChildBeanDefinition}.
 *
 * <p>The autowire constants match the ones defined in the
 * {@link org.springframework.beans.factory.config.AutowireCapableBeanFactory}
 * interface.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Mark Fisher
 * @see GenericBeanDefinition
 * @see RootBeanDefinition
 * @see ChildBeanDefinition
 */
@SuppressWarnings("serial")
public abstract class AbstractBeanDefinition extends BeanMetadataAttributeAccessor
		implements BeanDefinition, Cloneable {

	/**
	 * Constant for the default scope name: {@code ""}, equivalent to singleton
	 * status unless overridden from a parent bean definition (if applicable).
	 */
	public static final String SCOPE_DEFAULT = "";

	/**
     * autowireMode可以设置为这个值，表明当前bd对应的bean不会自动装配
	 * Constant that indicates no autowiring at all.
	 * @see #setAutowireMode
	 */
    //不进行依赖注入(默认的情况)。但是我们可以使用@Autowired 这个注解的去完成自动装配，这样即使autowireMode 为no也没关系。autowireMode和@Autowired是并列的，互相之间没有关系
    /**不进行依赖注入(默认的情况)。但是使用构造函数实例化bean的时候有||条件判断，这样即使autowireMode 为no也没关系。
     * if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
     *  		mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args))  {
     *       //委托给ConstructorResolver进行处理： ConstructorResolver(this).autowireConstructor
     *  	    return autowireConstructor(beanName, mbd, ctors, args);
     *  }
     */
	public static final int AUTOWIRE_NO = AutowireCapableBeanFactory.AUTOWIRE_NO;

	/**
     * autowireMode可以设置为这个值，表明当前bd对应的bean的属性根据名字进行自动装配
	 * Constant that indicates autowiring bean properties by name.
	 * @see #setAutowireMode
	 */
    //public static final int AUTOWIRE_AUTODETECT = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;
    //调用所有的setter方法依赖注入(根据name)
    /**
     * if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_NAME || mbd.getResolvedAutowireMode() == AUTOWIRE_BY_TYPE) {
     * 			MutablePropertyValues newPvs = new MutablePropertyValues(pvs);
     * 	    // Add property values based on autowire by name if applicable.
     * 	    if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_NAME) {
     * 	    	autowireByName(beanName, mbd, bw, newPvs);
     *                    }
     * 	    // Add property values based on autowire by type if applicable.
     * 	    if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_TYPE) {
     * 	    	autowireByType(beanName, mbd, bw, newPvs);
     *        }
     * 	    pvs = newPvs;
     * 	}
     */
    public static final int AUTOWIRE_BY_NAME = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;

	/**
     * autowireMode可以设置为这个值，表明当前bd对应的bean的属性根据类型进行自动装配
	 * Constant that indicates autowiring bean properties by type.
	 * @see #setAutowireMode
	 */
    //public static final int AUTOWIRE_AUTODETECT = AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE;
    //调用所有setters方法依赖注入(根据type)
    /**
     * if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_NAME || mbd.getResolvedAutowireMode() == AUTOWIRE_BY_TYPE) {
     * 			MutablePropertyValues newPvs = new MutablePropertyValues(pvs);
     * 	    // Add property values based on autowire by name if applicable.
     * 	    if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_NAME) {
     * 	    	autowireByName(beanName, mbd, bw, newPvs);
     *                    }
     * 	    // Add property values based on autowire by type if applicable.
     * 	    if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_TYPE) {
     * 	    	autowireByType(beanName, mbd, bw, newPvs);
     *        }
     * 	    pvs = newPvs;
     * 	}
     */
    public static final int AUTOWIRE_BY_TYPE = AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE;

	/**
     * autowireMode可以设置为这个值，表明当前bd对应的bean的构造函数依赖的属性自动装配
	 * Constant that indicates autowiring a constructor.
	 * @see #setAutowireMode
	 */
    //调用恰当的构造方法实例化的时候会完成依赖注入
    /**
     * Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
     * mbd.getResolvedAutowireMode()默认是AUTOWIRE_NO，但是因为是||的关系，所以仍然会进去这个分支
     * if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
     * 		mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args))  {
     *      //委托给ConstructorResolver进行处理： ConstructorResolver(this).autowireConstructor
     * 	    return autowireConstructor(beanName, mbd, ctors, args);
     * }
     */
    public static final int AUTOWIRE_CONSTRUCTOR = AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR;

	/**
     * 自动检测自动装配的方式
	 * Constant that indicates determining an appropriate autowire strategy
	 * through introspection of the bean class.
	 * @see #setAutowireMode
	 * @deprecated as of Spring 3.0: If you are using mixed autowiring strategies,
	 * use annotation-based autowiring for clearer demarcation of autowiring needs.
	 */
	@Deprecated
	public static final int AUTOWIRE_AUTODETECT = AutowireCapableBeanFactory.AUTOWIRE_AUTODETECT;

	//===========================依赖检查  begin===================================
    //    依赖检查要和自动装配结合使用，没有自动装配也就没有检查的必要了。
    //    在Bean被创建时Bean的属性（property）如果在配置文件Bean的定义中没有进行初始化赋值，
    //    默认情况下Spring对于没有进行初始化的属性（property）是不做检查的。
    //    但是很多情况下会要求Bean特定的属性必须进行初始化赋值，
    //    在Spring2.x中通过在bean标签中使用dependency-check属性设定由Spring进行强制检查的方式。
    //    denpendency-check属性有四个值, 下面已经列出了。
    //    当检查没有通过时会抛出org.springframework.beans.factory.UnsatisfiedDependencyException异常。

    //    在spring3+中已经放弃使用这个属性了，如果需要使用这个属性则需要使用spring2.5的xsd文件。
    //    而既然Spring3中放弃使用了dependency-check属性一定就会有替代它的功能出现。
    //    查了下资料，果然，在spring3中替代dependency-check有4条建议：
    //    1.Use constructors (constructor injection instead of setter injection) exclusively to ensure the right properties are set.
    //    使用构造方法（使用构造方法注入替代setter注入）专门用来确认特定属性被初始化
    //    2.Create setters with a dedicated init method implemented.
    //    用init方法初始化setter的属性
    //    3.Create setters with @Required annotation when the property is required.
    //    在需要强制进行初始化的setters上标注@Required
    //    4.Use @Autowired-driven injection which also implies a required property by default.
    //    使用@Autowired-driven 注入也可以实现
    /**
     * 如果不进行设置设就是Spring中dependency-check的默认值，不进行任何检查。
	 * Constant that indicates no dependency check at all.
	 * @see #setDependencyCheck
	 */
	public static final int DEPENDENCY_CHECK_NONE = 0;

	/**
     * 检查除简单类型属性以及集合类型属性外的引用类型属性
	 * Constant that indicates dependency checking for object references.
	 * @see #setDependencyCheck
	 */
	public static final int DEPENDENCY_CHECK_OBJECTS = 1;

	/**
     * 只检查简单类型属性以及集合类型属性
	 * Constant that indicates dependency checking for "simple" properties.
	 * @see #setDependencyCheck
	 * @see org.springframework.beans.BeanUtils#isSimpleProperty
	 */
	public static final int DEPENDENCY_CHECK_SIMPLE = 2;

	/**
     * 检查所有类型属性
	 * Constant that indicates dependency checking for all properties
	 * (object references as well as "simple" properties).
	 * @see #setDependencyCheck
	 */
	public static final int DEPENDENCY_CHECK_ALL = 3;
    //===========================依赖检查    end===================================

	/**
	 * Constant that indicates the container should attempt to infer the
	 * {@link #setDestroyMethodName destroy method name} for a bean as opposed to
	 * explicit specification of a method name. The value {@value} is specifically
	 * designed to include characters otherwise illegal in a method name, ensuring
	 * no possibility of collisions with legitimately named methods having the same
	 * name.
	 * <p>Currently, the method names detected during destroy method inference
	 * are "close" and "shutdown", if present on the specific bean class.
	 */
	public static final String INFER_METHOD = "(inferred)";


	@Nullable
	private volatile Object beanClass;

	@Nullable
	private String scope = SCOPE_DEFAULT;

	private boolean abstractFlag = false;

	private boolean lazyInit = false;

	//当前bd对应的bean依赖的属性，构造方法参数按照那种方法进行自动装配
	private int autowireMode = AUTOWIRE_NO;

	//依赖检查模式，spring3+已经不用这种方式，详见上面描述
	private int dependencyCheck = DEPENDENCY_CHECK_NONE;

	@Nullable
	private String[] dependsOn;

	private boolean autowireCandidate = true;

	private boolean primary = false;

	private final Map<String, AutowireCandidateQualifier> qualifiers = new LinkedHashMap<>();

	@Nullable
	private Supplier<?> instanceSupplier;

	private boolean nonPublicAccessAllowed = true;

	//在宽松模式下还是在严格模式下判断构造函数参数匹配度
	private boolean lenientConstructorResolution = true;

	@Nullable
    // 这里的factoryBean是指要调用factory method的实例，如果是静态工厂方法，则为{@code null}
    // factoryBeanName 是它的名字
	private String factoryBeanName;

	@Nullable
    // 工厂方法名字， spring中静态工厂方法，动态工厂方法，静态@bean method作为工厂方法，@bean method作为工厂方法都是工厂方法
	private String factoryMethodName;

	@Nullable
    //这个bean的构造方法参数值
    // 通过xml设置
    // 通过ImportBeanDefinitionRegistrar手工注册包含constructorArgumentValues的bd
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
     * <bean id="luban" class="com.luban.Luban">
     *     <constructor-arg index="0" value="str1"/>
     *     <constructor-arg index="1" value="1"/>
     *     <constructor-arg index="2" value="str2"/>
     * </bean>
     */
	private ConstructorArgumentValues constructorArgumentValues;

	@Nullable
    //这个bean的属性信息的集合
	private MutablePropertyValues propertyValues;

	@Nullable
    //构造bd时，lookup-method 和 replace-method的信息都会构造成MethodOverrides对象，保存在bd的methodOverrides这个属性里面
	private MethodOverrides methodOverrides;

	@Nullable
    //初始化方法名
	private String initMethodName;

	@Nullable
    //销毁的方法名
	private String destroyMethodName;

	//强制执行初始化方法标志, 默认true
	private boolean enforceInitMethod = true;
    //强制执行销毁方法标志, 默认true
	private boolean enforceDestroyMethod = true;

	private boolean synthetic = false;

	private int role = BeanDefinition.ROLE_APPLICATION;

	@Nullable
	private String description;

	@Nullable
	private Resource resource;


	/**
	 * Create a new AbstractBeanDefinition with default settings.
	 */
	protected AbstractBeanDefinition() {
		this(null, null);
	}

	/**
	 * Create a new AbstractBeanDefinition with the given
	 * constructor argument values and property values.
	 */
	protected AbstractBeanDefinition(@Nullable ConstructorArgumentValues cargs, @Nullable MutablePropertyValues pvs) {
		this.constructorArgumentValues = cargs;
		this.propertyValues = pvs;
	}

	/**
	 * Create a new AbstractBeanDefinition as a deep copy of the given
	 * bean definition.
	 * @param original the original bean definition to copy from
	 */
	protected AbstractBeanDefinition(BeanDefinition original) {
		setParentName(original.getParentName());
		setBeanClassName(original.getBeanClassName());
		setScope(original.getScope());
		setAbstract(original.isAbstract());
		setLazyInit(original.isLazyInit());
		setFactoryBeanName(original.getFactoryBeanName());
		setFactoryMethodName(original.getFactoryMethodName());
		setRole(original.getRole());
		setSource(original.getSource());
		copyAttributesFrom(original);

		if (original instanceof AbstractBeanDefinition) {
			AbstractBeanDefinition originalAbd = (AbstractBeanDefinition) original;
			if (originalAbd.hasBeanClass()) {
				setBeanClass(originalAbd.getBeanClass());
			}
			if (originalAbd.hasConstructorArgumentValues()) {
				setConstructorArgumentValues(new ConstructorArgumentValues(original.getConstructorArgumentValues()));
			}
			if (originalAbd.hasPropertyValues()) {
				setPropertyValues(new MutablePropertyValues(original.getPropertyValues()));
			}
			if (originalAbd.hasMethodOverrides()) {
				setMethodOverrides(new MethodOverrides(originalAbd.getMethodOverrides()));
			}
			setAutowireMode(originalAbd.getAutowireMode());
			setDependencyCheck(originalAbd.getDependencyCheck());
			setDependsOn(originalAbd.getDependsOn());
			setAutowireCandidate(originalAbd.isAutowireCandidate());
			setPrimary(originalAbd.isPrimary());
			copyQualifiersFrom(originalAbd);
			setInstanceSupplier(originalAbd.getInstanceSupplier());
			setNonPublicAccessAllowed(originalAbd.isNonPublicAccessAllowed());
			setLenientConstructorResolution(originalAbd.isLenientConstructorResolution());
			setInitMethodName(originalAbd.getInitMethodName());
			setEnforceInitMethod(originalAbd.isEnforceInitMethod());
			setDestroyMethodName(originalAbd.getDestroyMethodName());
			setEnforceDestroyMethod(originalAbd.isEnforceDestroyMethod());
			setSynthetic(originalAbd.isSynthetic());
			setResource(originalAbd.getResource());
		}
		else {
			setConstructorArgumentValues(new ConstructorArgumentValues(original.getConstructorArgumentValues()));
			setPropertyValues(new MutablePropertyValues(original.getPropertyValues()));
			setResourceDescription(original.getResourceDescription());
		}
	}


	/**
	 * Override settings in this bean definition (presumably a copied parent
	 * from a parent-child inheritance relationship) from the given bean
	 * definition (presumably the child).
	 * <ul>
	 * <li>Will override beanClass if specified in the given bean definition.
	 * <li>Will always take {@code abstract}, {@code scope},
	 * {@code lazyInit}, {@code autowireMode}, {@code dependencyCheck},
	 * and {@code dependsOn} from the given bean definition.
	 * <li>Will add {@code constructorArgumentValues}, {@code propertyValues},
	 * {@code methodOverrides} from the given bean definition to existing ones.
	 * <li>Will override {@code factoryBeanName}, {@code factoryMethodName},
	 * {@code initMethodName}, and {@code destroyMethodName} if specified
	 * in the given bean definition.
	 * </ul>
	 */
	public void overrideFrom(BeanDefinition other) {
		if (StringUtils.hasLength(other.getBeanClassName())) {
			setBeanClassName(other.getBeanClassName());
		}
		if (StringUtils.hasLength(other.getScope())) {
			setScope(other.getScope());
		}
		setAbstract(other.isAbstract());
		setLazyInit(other.isLazyInit());
		if (StringUtils.hasLength(other.getFactoryBeanName())) {
			setFactoryBeanName(other.getFactoryBeanName());
		}
		if (StringUtils.hasLength(other.getFactoryMethodName())) {
			setFactoryMethodName(other.getFactoryMethodName());
		}
		setRole(other.getRole());
		setSource(other.getSource());
		copyAttributesFrom(other);

		if (other instanceof AbstractBeanDefinition) {
			AbstractBeanDefinition otherAbd = (AbstractBeanDefinition) other;
			if (otherAbd.hasBeanClass()) {
				setBeanClass(otherAbd.getBeanClass());
			}
			if (otherAbd.hasConstructorArgumentValues()) {
				getConstructorArgumentValues().addArgumentValues(other.getConstructorArgumentValues());
			}
			if (otherAbd.hasPropertyValues()) {
				getPropertyValues().addPropertyValues(other.getPropertyValues());
			}
			if (otherAbd.hasMethodOverrides()) {
				getMethodOverrides().addOverrides(otherAbd.getMethodOverrides());
			}
			setAutowireMode(otherAbd.getAutowireMode());
			setDependencyCheck(otherAbd.getDependencyCheck());
			setDependsOn(otherAbd.getDependsOn());
			setAutowireCandidate(otherAbd.isAutowireCandidate());
			setPrimary(otherAbd.isPrimary());
			copyQualifiersFrom(otherAbd);
			setInstanceSupplier(otherAbd.getInstanceSupplier());
			setNonPublicAccessAllowed(otherAbd.isNonPublicAccessAllowed());
			setLenientConstructorResolution(otherAbd.isLenientConstructorResolution());
			if (otherAbd.getInitMethodName() != null) {
				setInitMethodName(otherAbd.getInitMethodName());
				setEnforceInitMethod(otherAbd.isEnforceInitMethod());
			}
			if (otherAbd.getDestroyMethodName() != null) {
				setDestroyMethodName(otherAbd.getDestroyMethodName());
				setEnforceDestroyMethod(otherAbd.isEnforceDestroyMethod());
			}
			setSynthetic(otherAbd.isSynthetic());
			setResource(otherAbd.getResource());
		}
		else {
			getConstructorArgumentValues().addArgumentValues(other.getConstructorArgumentValues());
			getPropertyValues().addPropertyValues(other.getPropertyValues());
			setResourceDescription(other.getResourceDescription());
		}
	}

	/**
	 * Apply the provided default values to this bean.
	 * @param defaults the defaults to apply
	 */
	public void applyDefaults(BeanDefinitionDefaults defaults) {
		//是否延迟加载
		setLazyInit(defaults.isLazyInit());
		//自动装配规则
		setAutowireMode(defaults.getAutowireMode());
		setDependencyCheck(defaults.getDependencyCheck());
		setInitMethodName(defaults.getInitMethodName());
		setEnforceInitMethod(false);
		setDestroyMethodName(defaults.getDestroyMethodName());
		setEnforceDestroyMethod(false);
	}


	/**
	 * Specify the bean class name of this bean definition.
	 */
	@Override
	public void setBeanClassName(@Nullable String beanClassName) {
		this.beanClass = beanClassName;
	}

	/**
	 * Return the current bean class name of this bean definition.
	 */
	@Override
	@Nullable
	public String getBeanClassName() {
		Object beanClassObject = this.beanClass;
		if (beanClassObject instanceof Class) {
			return ((Class<?>) beanClassObject).getName();
		}
		else {
			return (String) beanClassObject;
		}
	}

	/**
	 * Specify the class for this bean.
	 */
	public void setBeanClass(@Nullable Class<?> beanClass) {
		this.beanClass = beanClass;
	}

	/**
	 * Return the class of the wrapped bean, if already resolved.
	 * @return the bean class, or {@code null} if none defined
	 * @throws IllegalStateException if the bean definition does not define a bean class,
	 * or a specified bean class name has not been resolved into an actual Class
	 */
	public Class<?> getBeanClass() throws IllegalStateException {
		Object beanClassObject = this.beanClass;
		if (beanClassObject == null) {
			throw new IllegalStateException("No bean class specified on bean definition");
		}
		if (!(beanClassObject instanceof Class)) {
			throw new IllegalStateException(
					"Bean class name [" + beanClassObject + "] has not been resolved into an actual Class");
		}
		return (Class<?>) beanClassObject;
	}

	/**
	 * Return whether this definition specifies a bean class.
	 */
	public boolean hasBeanClass() {
		return (this.beanClass instanceof Class);
	}

	/**
	 * Determine the class of the wrapped bean, resolving it from a
	 * specified class name if necessary. Will also reload a specified
	 * Class from its name when called with the bean class already resolved.
	 * @param classLoader the ClassLoader to use for resolving a (potential) class name
	 * @return the resolved bean class
	 * @throws ClassNotFoundException if the class name could be resolved
	 */
	@Nullable
	public Class<?> resolveBeanClass(@Nullable ClassLoader classLoader) throws ClassNotFoundException {
		String className = getBeanClassName();
		if (className == null) {
			return null;
		}
		Class<?> resolvedClass = ClassUtils.forName(className, classLoader);
		this.beanClass = resolvedClass;
		return resolvedClass;
	}

	/**
	 * Set the name of the target scope for the bean.
	 * <p>The default is singleton status, although this is only applied once
	 * a bean definition becomes active in the containing factory. A bean
	 * definition may eventually inherit its scope from a parent bean definition.
	 * For this reason, the default scope name is an empty string (i.e., {@code ""}),
	 * with singleton status being assumed until a resolved scope is set.
	 * @see #SCOPE_SINGLETON
	 * @see #SCOPE_PROTOTYPE
	 */
	@Override
	public void setScope(@Nullable String scope) {
		this.scope = scope;
	}

	/**
	 * Return the name of the target scope for the bean.
	 */
	@Override
	@Nullable
	public String getScope() {
		return this.scope;
	}

	/**
	 * Return whether this a <b>Singleton</b>, with a single shared instance
	 * returned from all calls.
	 * @see #SCOPE_SINGLETON
	 */
	@Override
	public boolean isSingleton() {
		return SCOPE_SINGLETON.equals(this.scope) || SCOPE_DEFAULT.equals(this.scope);
	}

	/**
	 * Return whether this a <b>Prototype</b>, with an independent instance
	 * returned for each call.
	 * @see #SCOPE_PROTOTYPE
	 */
	@Override
	public boolean isPrototype() {
		return SCOPE_PROTOTYPE.equals(this.scope);
	}

	/**
	 * Set if this bean is "abstract", i.e. not meant to be instantiated itself but
	 * rather just serving as parent for concrete child bean definitions.
	 * <p>Default is "false". Specify true to tell the bean factory to not try to
	 * instantiate that particular bean in any case.
	 */
	public void setAbstract(boolean abstractFlag) {
		this.abstractFlag = abstractFlag;
	}

	/**
	 * Return whether this bean is "abstract", i.e. not meant to be instantiated
	 * itself but rather just serving as parent for concrete child bean definitions.
	 */
	@Override
	public boolean isAbstract() {
		return this.abstractFlag;
	}

	/**
	 * Set whether this bean should be lazily initialized.
	 * <p>If {@code false}, the bean will get instantiated on startup by bean
	 * factories that perform eager initialization of singletons.
	 */
	@Override
	public void setLazyInit(boolean lazyInit) {
		this.lazyInit = lazyInit;
	}

	/**
	 * Return whether this bean should be lazily initialized, i.e. not
	 * eagerly instantiated on startup. Only applicable to a singleton bean.
	 */
	@Override
	public boolean isLazyInit() {
		return this.lazyInit;
	}

	/**
	 * Set the autowire mode. This determines whether any automagical detection
	 * and setting of bean references will happen. Default is AUTOWIRE_NO,
	 * which means there's no autowire.
	 * @param autowireMode the autowire mode to set.
	 * Must be one of the constants defined in this class.
	 * @see #AUTOWIRE_NO
	 * @see #AUTOWIRE_BY_NAME
	 * @see #AUTOWIRE_BY_TYPE
	 * @see #AUTOWIRE_CONSTRUCTOR
	 * @see #AUTOWIRE_AUTODETECT
	 */
	public void setAutowireMode(int autowireMode) {
		this.autowireMode = autowireMode;
	}

	/**
	 * Return the autowire mode as specified in the bean definition.
	 */
	public int getAutowireMode() {
		return this.autowireMode;
	}

	/**
	 * Return the resolved autowire code,
	 * (resolving AUTOWIRE_AUTODETECT to AUTOWIRE_CONSTRUCTOR or AUTOWIRE_BY_TYPE).
	 * @see #AUTOWIRE_AUTODETECT
	 * @see #AUTOWIRE_CONSTRUCTOR
	 * @see #AUTOWIRE_BY_TYPE
	 */
	public int getResolvedAutowireMode() {
		if (this.autowireMode == AUTOWIRE_AUTODETECT) {
			// Work out whether to apply setter autowiring or constructor autowiring.
			// If it has a no-arg constructor it's deemed to be setter autowiring,
			// otherwise we'll try constructor autowiring.
			Constructor<?>[] constructors = getBeanClass().getConstructors();
			for (Constructor<?> constructor : constructors) {
				if (constructor.getParameterCount() == 0) {
					return AUTOWIRE_BY_TYPE;
				}
			}
			return AUTOWIRE_CONSTRUCTOR;
		}
		else {
			return this.autowireMode;
		}
	}

	/**
	 * Set the dependency check code.
	 * @param dependencyCheck the code to set.
	 * Must be one of the four constants defined in this class.
	 * @see #DEPENDENCY_CHECK_NONE
	 * @see #DEPENDENCY_CHECK_OBJECTS
	 * @see #DEPENDENCY_CHECK_SIMPLE
	 * @see #DEPENDENCY_CHECK_ALL
	 */
	public void setDependencyCheck(int dependencyCheck) {
		this.dependencyCheck = dependencyCheck;
	}

	/**
	 * Return the dependency check code.
	 */
	public int getDependencyCheck() {
		return this.dependencyCheck;
	}

	/**
	 * Set the names of the beans that this bean depends on being initialized.
	 * The bean factory will guarantee that these beans get initialized first.
	 * <p>Note that dependencies are normally expressed through bean properties or
	 * constructor arguments. This property should just be necessary for other kinds
	 * of dependencies like statics (*ugh*) or database preparation on startup.
	 */
	@Override
	public void setDependsOn(@Nullable String... dependsOn) {
		this.dependsOn = dependsOn;
	}

	/**
	 * Return the bean names that this bean depends on.
	 */
	@Override
	@Nullable
	public String[] getDependsOn() {
		return this.dependsOn;
	}

	/**
	 * Set whether this bean is a candidate for getting autowired into some other bean.
	 * <p>Note that this flag is designed to only affect type-based autowiring.
	 * It does not affect explicit references by name, which will get resolved even
	 * if the specified bean is not marked as an autowire candidate. As a consequence,
	 * autowiring by name will nevertheless inject a bean if the name matches.
	 * @see #AUTOWIRE_BY_TYPE
	 * @see #AUTOWIRE_BY_NAME
	 */
	@Override
	public void setAutowireCandidate(boolean autowireCandidate) {
		this.autowireCandidate = autowireCandidate;
	}

	/**
	 * Return whether this bean is a candidate for getting autowired into some other bean.
	 */
	@Override
	public boolean isAutowireCandidate() {
		return this.autowireCandidate;
	}

	/**
	 * Set whether this bean is a primary autowire candidate.
	 * <p>If this value is {@code true} for exactly one bean among multiple
	 * matching candidates, it will serve as a tie-breaker.
	 */
	@Override
	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	/**
	 * Return whether this bean is a primary autowire candidate.
	 */
	@Override
	public boolean isPrimary() {
		return this.primary;
	}

	/**
	 * Register a qualifier to be used for autowire candidate resolution,
	 * keyed by the qualifier's type name.
	 * @see AutowireCandidateQualifier#getTypeName()
	 */
	public void addQualifier(AutowireCandidateQualifier qualifier) {
		this.qualifiers.put(qualifier.getTypeName(), qualifier);
	}

	/**
	 * Return whether this bean has the specified qualifier.
	 */
	public boolean hasQualifier(String typeName) {
		return this.qualifiers.keySet().contains(typeName);
	}

	/**
	 * Return the qualifier mapped to the provided type name.
	 */
	@Nullable
	public AutowireCandidateQualifier getQualifier(String typeName) {
		return this.qualifiers.get(typeName);
	}

	/**
	 * Return all registered qualifiers.
	 * @return the Set of {@link AutowireCandidateQualifier} objects.
	 */
	public Set<AutowireCandidateQualifier> getQualifiers() {
		return new LinkedHashSet<>(this.qualifiers.values());
	}

	/**
	 * Copy the qualifiers from the supplied AbstractBeanDefinition to this bean definition.
	 * @param source the AbstractBeanDefinition to copy from
	 */
	public void copyQualifiersFrom(AbstractBeanDefinition source) {
		Assert.notNull(source, "Source must not be null");
		this.qualifiers.putAll(source.qualifiers);
	}

	/**
	 * Specify a callback for creating an instance of the bean,
	 * as an alternative to a declaratively specified factory method.
	 * <p>If such a callback is set, it will override any other constructor
	 * or factory method metadata. However, bean property population and
	 * potential annotation-driven injection will still apply as usual.
	 * @since 5.0
	 * @see #setConstructorArgumentValues(ConstructorArgumentValues)
	 * @see #setPropertyValues(MutablePropertyValues)
	 */
	public void setInstanceSupplier(@Nullable Supplier<?> instanceSupplier) {
		this.instanceSupplier = instanceSupplier;
	}

	/**
	 * Return a callback for creating an instance of the bean, if any.
	 * @since 5.0
	 */
	@Nullable
	public Supplier<?> getInstanceSupplier() {
		return this.instanceSupplier;
	}

	/**
	 * Specify whether to allow access to non-public constructors and methods,
	 * for the case of externalized metadata pointing to those. The default is
	 * {@code true}; switch this to {@code false} for public access only.
	 * <p>This applies to constructor resolution, factory method resolution,
	 * and also init/destroy methods. Bean property accessors have to be public
	 * in any case and are not affected by this setting.
	 * <p>Note that annotation-driven configuration will still access non-public
	 * members as far as they have been annotated. This setting applies to
	 * externalized metadata in this bean definition only.
	 */
	public void setNonPublicAccessAllowed(boolean nonPublicAccessAllowed) {
		this.nonPublicAccessAllowed = nonPublicAccessAllowed;
	}

	/**
	 * Return whether to allow access to non-public constructors and methods.
	 */
	public boolean isNonPublicAccessAllowed() {
		return this.nonPublicAccessAllowed;
	}

	/**
	 * Specify whether to resolve constructors in lenient mode ({@code true},
	 * which is the default) or to switch to strict resolution (throwing an exception
	 * in case of ambiguous constructors that all match when converting the arguments,
	 * whereas lenient mode would use the one with the 'closest' type matches).
	 */
	public void setLenientConstructorResolution(boolean lenientConstructorResolution) {
		this.lenientConstructorResolution = lenientConstructorResolution;
	}

	/**
	 * Return whether to resolve constructors in lenient mode or in strict mode.
	 */
	//返回是在宽松模式下还是在严格模式下判断构造函数参数匹配度
	public boolean isLenientConstructorResolution() {
		return this.lenientConstructorResolution;
	}

	/**
	 * Specify the factory bean to use, if any.
	 * This the name of the bean to call the specified factory method on.
	 * @see #setFactoryMethodName
	 */
	@Override
	public void setFactoryBeanName(@Nullable String factoryBeanName) {
		this.factoryBeanName = factoryBeanName;
	}

	/**
	 * Return the factory bean name, if any.
	 */
	@Override
	@Nullable
	public String getFactoryBeanName() {
		return this.factoryBeanName;
	}

	/**
	 * Specify a factory method, if any. This method will be invoked with
	 * constructor arguments, or with no arguments if none are specified.
	 * The method will be invoked on the specified factory bean, if any,
	 * or otherwise as a static method on the local bean class.
	 * @see #setFactoryBeanName
	 * @see #setBeanClassName
	 */
	@Override
	public void setFactoryMethodName(@Nullable String factoryMethodName) {
		this.factoryMethodName = factoryMethodName;
	}

	/**
	 * Return a factory method, if any.
	 */
	@Override
	@Nullable
	public String getFactoryMethodName() {
		return this.factoryMethodName;
	}

	/**
	 * Specify constructor argument values for this bean.
	 */
	public void setConstructorArgumentValues(ConstructorArgumentValues constructorArgumentValues) {
		this.constructorArgumentValues = constructorArgumentValues;
	}

	/**
	 * Return constructor argument values for this bean (never {@code null}).
	 */
	@Override
	public ConstructorArgumentValues getConstructorArgumentValues() {
		if (this.constructorArgumentValues == null) {
			this.constructorArgumentValues = new ConstructorArgumentValues();
		}
		return this.constructorArgumentValues;
	}

	/**
	 * Return if there are constructor argument values defined for this bean.
	 */
	@Override
	public boolean hasConstructorArgumentValues() {
		return (this.constructorArgumentValues != null && !this.constructorArgumentValues.isEmpty());
	}

	/**
	 * Specify property values for this bean, if any.
	 */
	public void setPropertyValues(MutablePropertyValues propertyValues) {
		this.propertyValues = propertyValues;
	}

	/**
	 * Return property values for this bean (never {@code null}).
	 */
	@Override
	public MutablePropertyValues getPropertyValues() {
		if (this.propertyValues == null) {
			this.propertyValues = new MutablePropertyValues();
		}
		return this.propertyValues;
	}

	/**
	 * Return if there are property values values defined for this bean.
	 * @since 5.0.2
	 */
	@Override
	public boolean hasPropertyValues() {
		return (this.propertyValues != null && !this.propertyValues.isEmpty());
	}

	/**
	 * Specify method overrides for the bean, if any.
	 */
	public void setMethodOverrides(MethodOverrides methodOverrides) {
		this.methodOverrides = methodOverrides;
	}

	/**
	 * Return information about methods to be overridden by the IoC
	 * container. This will be empty if there are no method overrides.
	 * <p>Never returns {@code null}.
	 */
	public MethodOverrides getMethodOverrides() {
		if (this.methodOverrides == null) {
			this.methodOverrides = new MethodOverrides();
		}
		return this.methodOverrides;
	}

	/**
	 * Return if there are method overrides defined for this bean.
	 * @since 5.0.2
	 */
	public boolean hasMethodOverrides() {
		return (this.methodOverrides != null && !this.methodOverrides.isEmpty());
	}

	/**
	 * Set the name of the initializer method.
	 * <p>The default is {@code null} in which case there is no initializer method.
	 */
	public void setInitMethodName(@Nullable String initMethodName) {
		this.initMethodName = initMethodName;
	}

	/**
	 * Return the name of the initializer method.
	 */
	@Nullable
	public String getInitMethodName() {
		return this.initMethodName;
	}

	/**
	 * Specify whether or not the configured init method is the default.
	 * <p>The default value is {@code false}.
	 * @see #setInitMethodName
	 */
	public void setEnforceInitMethod(boolean enforceInitMethod) {
		this.enforceInitMethod = enforceInitMethod;
	}

	/**
	 * Indicate whether the configured init method is the default.
	 * @see #getInitMethodName()
	 */
	public boolean isEnforceInitMethod() {
		return this.enforceInitMethod;
	}

	/**
	 * Set the name of the destroy method.
	 * <p>The default is {@code null} in which case there is no destroy method.
	 */
	public void setDestroyMethodName(@Nullable String destroyMethodName) {
		this.destroyMethodName = destroyMethodName;
	}

	/**
	 * Return the name of the destroy method.
	 */
	@Nullable
	public String getDestroyMethodName() {
		return this.destroyMethodName;
	}

	/**
	 * Specify whether or not the configured destroy method is the default.
	 * <p>The default value is {@code false}.
	 * @see #setDestroyMethodName
	 */
	public void setEnforceDestroyMethod(boolean enforceDestroyMethod) {
		this.enforceDestroyMethod = enforceDestroyMethod;
	}

	/**
	 * Indicate whether the configured destroy method is the default.
	 * @see #getDestroyMethodName
	 */
	public boolean isEnforceDestroyMethod() {
		return this.enforceDestroyMethod;
	}

	/**
	 * Set whether this bean definition is 'synthetic', that is, not defined
	 * by the application itself (for example, an infrastructure bean such
	 * as a helper for auto-proxying, created through {@code <aop:config>}).
	 */
	public void setSynthetic(boolean synthetic) {
		this.synthetic = synthetic;
	}

	/**
	 * Return whether this bean definition is 'synthetic', that is,
	 * not defined by the application itself.
     * 判断是否是spring为编译器产生的属性构造的bd，而不是程序本身定义的
	 */
	public boolean isSynthetic() {
		return this.synthetic;
	}

	/**
	 * Set the role hint for this {@code BeanDefinition}.
	 */
	public void setRole(int role) {
		this.role = role;
	}

	/**
	 * Return the role hint for this {@code BeanDefinition}.
	 */
	@Override
	public int getRole() {
		return this.role;
	}

	/**
	 * Set a human-readable description of this bean definition.
	 */
	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	/**
	 * Return a human-readable description of this bean definition.
	 */
	@Override
	@Nullable
	public String getDescription() {
		return this.description;
	}

	/**
	 * Set the resource that this bean definition came from
	 * (for the purpose of showing context in case of errors).
	 */
	public void setResource(@Nullable Resource resource) {
		this.resource = resource;
	}

	/**
	 * Return the resource that this bean definition came from.
	 */
	@Nullable
	public Resource getResource() {
		return this.resource;
	}

	/**
	 * Set a description of the resource that this bean definition
	 * came from (for the purpose of showing context in case of errors).
	 */
	public void setResourceDescription(@Nullable String resourceDescription) {
		this.resource = (resourceDescription != null ? new DescriptiveResource(resourceDescription) : null);
	}

	/**
	 * Return a description of the resource that this bean definition
	 * came from (for the purpose of showing context in case of errors).
	 */
	@Override
	@Nullable
	public String getResourceDescription() {
		return (this.resource != null ? this.resource.getDescription() : null);
	}

	/**
	 * Set the originating (e.g. decorated) BeanDefinition, if any.
	 */
	public void setOriginatingBeanDefinition(BeanDefinition originatingBd) {
		this.resource = new BeanDefinitionResource(originatingBd);
	}

	/**
	 * Return the originating BeanDefinition, or {@code null} if none.
	 * Allows for retrieving the decorated bean definition, if any.
	 * <p>Note that this method returns the immediate originator. Iterate through the
	 * originator chain to find the original BeanDefinition as defined by the user.
	 */
	@Override
	@Nullable
	public BeanDefinition getOriginatingBeanDefinition() {
		return (this.resource instanceof BeanDefinitionResource ?
				((BeanDefinitionResource) this.resource).getBeanDefinition() : null);
	}

	/**
	 * Validate this bean definition.
	 * @throws BeanDefinitionValidationException in case of validation failure
	 */
	public void validate() throws BeanDefinitionValidationException {
		if (hasMethodOverrides() && getFactoryMethodName() != null) {
			throw new BeanDefinitionValidationException(
					"Cannot combine static factory method with method overrides: " +
					"the static factory method must create the instance");
		}

		if (hasBeanClass()) {
			prepareMethodOverrides();
		}
	}

	/**
     * bean标签中的lookup-method和replace-method属性就是放在BeanDefinition的methodOverrides属性中
     * 这里主要就是验证和准备 overrides methods
	 * Validate and prepare the method overrides defined for this bean.
	 * Checks for existence of a method with the specified name.
	 * @throws BeanDefinitionValidationException in case of validation failure
	 */
	public void prepareMethodOverrides() throws BeanDefinitionValidationException {
		// Check that lookup methods exists.
		if (hasMethodOverrides()) {
			Set<MethodOverride> overrides = getMethodOverrides().getOverrides();
			synchronized (overrides) {
				for (MethodOverride mo : overrides) {
					prepareMethodOverride(mo);
				}
			}
		}
	}

	/**
     * 这里主要就是验证和准备 overrides methods
	 * Validate and prepare the given method override.
	 * Checks for existence of a method with the specified name,
	 * marking it as not overloaded if none found.
	 * @param mo the MethodOverride object to validate
	 * @throws BeanDefinitionValidationException in case of validation failure
	 */
	protected void prepareMethodOverride(MethodOverride mo) throws BeanDefinitionValidationException {
		int count = ClassUtils.getMethodCountForName(getBeanClass(), mo.getMethodName());
		if (count == 0) {
			throw new BeanDefinitionValidationException(
					"Invalid method override: no method with name '" + mo.getMethodName() +
					"' on class [" + getBeanClassName() + "]");
		}
		else if (count == 1) {
			// Mark override as not overloaded, to avoid the overhead of arg type checking.
			mo.setOverloaded(false);
		}
	}


	/**
	 * Public declaration of Object's {@code clone()} method.
	 * Delegates to {@link #cloneBeanDefinition()}.
	 * @see Object#clone()
	 */
	@Override
	public Object clone() {
		return cloneBeanDefinition();
	}

	/**
	 * Clone this bean definition.
	 * To be implemented by concrete subclasses.
	 * @return the cloned bean definition object
	 */
	public abstract AbstractBeanDefinition cloneBeanDefinition();

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AbstractBeanDefinition)) {
			return false;
		}

		AbstractBeanDefinition that = (AbstractBeanDefinition) other;

		if (!ObjectUtils.nullSafeEquals(getBeanClassName(), that.getBeanClassName())) return false;
		if (!ObjectUtils.nullSafeEquals(this.scope, that.scope)) return false;
		if (this.abstractFlag != that.abstractFlag) return false;
		if (this.lazyInit != that.lazyInit) return false;

		if (this.autowireMode != that.autowireMode) return false;
		if (this.dependencyCheck != that.dependencyCheck) return false;
		if (!Arrays.equals(this.dependsOn, that.dependsOn)) return false;
		if (this.autowireCandidate != that.autowireCandidate) return false;
		if (!ObjectUtils.nullSafeEquals(this.qualifiers, that.qualifiers)) return false;
		if (this.primary != that.primary) return false;

		if (this.nonPublicAccessAllowed != that.nonPublicAccessAllowed) return false;
		if (this.lenientConstructorResolution != that.lenientConstructorResolution) return false;
		if (!ObjectUtils.nullSafeEquals(this.constructorArgumentValues, that.constructorArgumentValues)) return false;
		if (!ObjectUtils.nullSafeEquals(this.propertyValues, that.propertyValues)) return false;
		if (!ObjectUtils.nullSafeEquals(this.methodOverrides, that.methodOverrides)) return false;

		if (!ObjectUtils.nullSafeEquals(this.factoryBeanName, that.factoryBeanName)) return false;
		if (!ObjectUtils.nullSafeEquals(this.factoryMethodName, that.factoryMethodName)) return false;
		if (!ObjectUtils.nullSafeEquals(this.initMethodName, that.initMethodName)) return false;
		if (this.enforceInitMethod != that.enforceInitMethod) return false;
		if (!ObjectUtils.nullSafeEquals(this.destroyMethodName, that.destroyMethodName)) return false;
		if (this.enforceDestroyMethod != that.enforceDestroyMethod) return false;

		if (this.synthetic != that.synthetic) return false;
		if (this.role != that.role) return false;

		return super.equals(other);
	}

	@Override
	public int hashCode() {
		int hashCode = ObjectUtils.nullSafeHashCode(getBeanClassName());
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.scope);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.constructorArgumentValues);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.propertyValues);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.factoryBeanName);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.factoryMethodName);
		hashCode = 29 * hashCode + super.hashCode();
		return hashCode;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("class [");
		sb.append(getBeanClassName()).append("]");
		sb.append("; scope=").append(this.scope);
		sb.append("; abstract=").append(this.abstractFlag);
		sb.append("; lazyInit=").append(this.lazyInit);
		sb.append("; autowireMode=").append(this.autowireMode);
		sb.append("; dependencyCheck=").append(this.dependencyCheck);
		sb.append("; autowireCandidate=").append(this.autowireCandidate);
		sb.append("; primary=").append(this.primary);
		sb.append("; factoryBeanName=").append(this.factoryBeanName);
		sb.append("; factoryMethodName=").append(this.factoryMethodName);
		sb.append("; initMethodName=").append(this.initMethodName);
		sb.append("; destroyMethodName=").append(this.destroyMethodName);
		if (this.resource != null) {
			sb.append("; defined in ").append(this.resource.getDescription());
		}
		return sb.toString();
	}

}

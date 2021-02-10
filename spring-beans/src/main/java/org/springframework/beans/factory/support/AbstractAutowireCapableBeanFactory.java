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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorUtils;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.Aware;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.swing.*;

/**
 * Abstract bean factory superclass that implements default bean creation,
 * with the full capabilities specified by the {@link RootBeanDefinition} class.
 * Implements the {@link org.springframework.beans.factory.config.AutowireCapableBeanFactory}
 * interface in addition to AbstractBeanFactory's {@link #createBean} method.
 *
 * <p>Provides bean creation (with constructor resolution), property population,
 * wiring (including autowiring), and initialization. Handles runtime bean
 * references, resolves managed collections, calls initialization methods, etc.
 * Supports autowiring constructors, properties by name, and properties by type.
 *
 * <p>The main template method to be implemented by subclasses is
 * {@link #resolveDependency(DependencyDescriptor, String, Set, TypeConverter)},
 * used for autowiring by type. In case of a factory which is capable of searching
 * its bean definitions, matching beans will typically be implemented through such
 * a search. For other factory styles, simplified matching algorithms can be implemented.
 *
 * <p>Note that this class does <i>not</i> assume or implement bean definition
 * registry capabilities. See {@link DefaultListableBeanFactory} for an implementation
 * of the {@link org.springframework.beans.factory.ListableBeanFactory} and
 * {@link BeanDefinitionRegistry} interfaces, which represent the API and SPI
 * view of such a factory, respectively.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Mark Fisher
 * @author Costin Leau
 * @author Chris Beams
 * @author Sam Brannen
 * @since 13.02.2004
 * @see RootBeanDefinition
 * @see DefaultListableBeanFactory
 * @see BeanDefinitionRegistry
 */
//继承了AbstractBeanFactory的能力，并且实现了AutowireCapableBeanFactory，所以也拥有自动注入的能力
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory
        implements AutowireCapableBeanFactory {

    /** Strategy for creating bean instances */
    //创建bean实例的策略对象 ： CglibSubclassingInstantiationStrategy是BeanFactory默认使用的bean实例化策略对象
    private InstantiationStrategy instantiationStrategy = new CglibSubclassingInstantiationStrategy();

    /** Resolver strategy for method parameter names */
    @Nullable
    //获取方法和构造函数的参数名的工具 ：默认使用DefaultParameterNameDiscoverer
    private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /** Whether to automatically try to resolve circular references between beans */
    // 是否自动尝试解析bean之间的循环引用
    private boolean allowCircularReferences = true;

    /**
     * Whether to resort to injecting a raw bean instance in case of circular reference,
     * even if the injected bean eventually got wrapped.
     */
    //在循环引用中是否注入原始的bean实例，即使bean实例最终会被封装(或者代理)
    private boolean allowRawInjectionDespiteWrapping = false;

    /**
     * Dependency types to ignore on dependency check and autowire, as Set of
     * Class objects: for example, String. Default is none.
     */
    //依赖项检查和自动装配时忽略的依赖项类型，比如String
    private final Set<Class<?>> ignoredDependencyTypes = new HashSet<>();

    /**
     * Dependency interfaces to ignore on dependency check and autowire, as Set of
     * Class objects. By default, only the BeanFactory interface is ignored.
     */
    //依赖项检查和自动装配时忽略的依赖项接口, AbstractAutowireCapableBeanFactory() 构造函数中，往ignoredDependencyInterfaces添加了3个Class : BeanNameAware.class BeanFactoryAware.class BeanClassLoaderAware.class
    private final Set<Class<?>> ignoredDependencyInterfaces = new HashSet<>();

    /**
     * 当前创建的bean的名称
     * 参考AbstractAutowireCapableBeanFactory.obtainFromSupplier和Supplier接口
     * 只在obtainFromSupplier方法中设置currentlyCreatedBean和删除
     * The name of the currently created bean, for implicit dependency registration
     * on getBean etc invocations triggered from a user-specified Supplier callback.
     */
    private final NamedThreadLocal<String> currentlyCreatedBean = new NamedThreadLocal<>("Currently created bean");

    /** Cache of unfinished FactoryBean instances: FactoryBean name to BeanWrapper */
    //缓存没有完成的factorybean实例 ：factorybean 名字到BeanWrapper的映射的结合
    private final ConcurrentMap<String, BeanWrapper> factoryBeanInstanceCache = new ConcurrentHashMap<>(16);

    /** Cache of filtered PropertyDescriptors: bean Class to PropertyDescriptor array */
    //过滤后的PropertyDescriptor的缓存
    private final ConcurrentMap<Class<?>, PropertyDescriptor[]> filteredPropertyDescriptorsCache =
            new ConcurrentHashMap<>(256);


    /**
     * Create a new AbstractAutowireCapableBeanFactory.
     */
    public AbstractAutowireCapableBeanFactory() {
        super();
        ignoreDependencyInterface(BeanNameAware.class);
        ignoreDependencyInterface(BeanFactoryAware.class);
        ignoreDependencyInterface(BeanClassLoaderAware.class);
    }

    /**
     * Create a new AbstractAutowireCapableBeanFactory with the given parent.
     * @param parentBeanFactory parent bean factory, or {@code null} if none
     */
    public AbstractAutowireCapableBeanFactory(@Nullable BeanFactory parentBeanFactory) {
        this();
        setParentBeanFactory(parentBeanFactory);
    }


    /**
     * Set the instantiation strategy to use for creating bean instances.
     * Default is CglibSubclassingInstantiationStrategy.
     * @see CglibSubclassingInstantiationStrategy
     */
    public void setInstantiationStrategy(InstantiationStrategy instantiationStrategy) {
        this.instantiationStrategy = instantiationStrategy;
    }

    /**
     * 返回正在创建的bean 的实例化策略
     * Return the instantiation strategy to use for creating bean instances.
     */
    protected InstantiationStrategy getInstantiationStrategy() {
        return this.instantiationStrategy;
    }

    /**
     * Set the ParameterNameDiscoverer to use for resolving method parameter
     * names if needed (e.g. for constructor names).
     * <p>Default is a {@link DefaultParameterNameDiscoverer}.
     */
    public void setParameterNameDiscoverer(@Nullable ParameterNameDiscoverer parameterNameDiscoverer) {
        this.parameterNameDiscoverer = parameterNameDiscoverer;
    }

    /**
     * Return the ParameterNameDiscoverer to use for resolving method parameter
     * names if needed.
     */
    @Nullable
    protected ParameterNameDiscoverer getParameterNameDiscoverer() {
        return this.parameterNameDiscoverer;
    }

    /**
     * Set whether to allow circular references between beans - and automatically
     * try to resolve them.
     * <p>Note that circular reference resolution means that one of the involved beans
     * will receive a reference to another bean that is not fully initialized yet.
     * This can lead to subtle and not-so-subtle side effects on initialization;
     * it does work fine for many scenarios, though.
     * <p>Default is "true". Turn this off to throw an exception when encountering
     * a circular reference, disallowing them completely.
     * <p><b>NOTE:</b> It is generally recommended to not rely on circular references
     * between your beans. Refactor your application logic to have the two beans
     * involved delegate to a third bean that encapsulates their common logic.
     */
    public void setAllowCircularReferences(boolean allowCircularReferences) {
        this.allowCircularReferences = allowCircularReferences;
    }

    /**
     * Set whether to allow the raw injection of a bean instance into some other
     * bean's property, despite the injected bean eventually getting wrapped
     * (for example, through AOP auto-proxying).
     * <p>This will only be used as a last resort in case of a circular reference
     * that cannot be resolved otherwise: essentially, preferring a raw instance
     * getting injected over a failure of the entire bean wiring process.
     * <p>Default is "false", as of Spring 2.0. Turn this on to allow for non-wrapped
     * raw beans injected into some of your references, which was Spring 1.2's
     * (arguably unclean) default behavior.
     * <p><b>NOTE:</b> It is generally recommended to not rely on circular references
     * between your beans, in particular with auto-proxying involved.
     * @see #setAllowCircularReferences
     */
    public void setAllowRawInjectionDespiteWrapping(boolean allowRawInjectionDespiteWrapping) {
        this.allowRawInjectionDespiteWrapping = allowRawInjectionDespiteWrapping;
    }

    /**
     * Ignore the given dependency type for autowiring:
     * for example, String. Default is none.
     */
    public void ignoreDependencyType(Class<?> type) {
        this.ignoredDependencyTypes.add(type);
    }

    /**
     * Ignore the given dependency interface for autowiring.
     * <p>This will typically be used by application contexts to register
     * dependencies that are resolved in other ways, like BeanFactory through
     * BeanFactoryAware or ApplicationContext through ApplicationContextAware.
     * <p>By default, only the BeanFactoryAware interface is ignored.
     * For further types to ignore, invoke this method for each type.
     * @see org.springframework.beans.factory.BeanFactoryAware
     * @see org.springframework.context.ApplicationContextAware
     */
    public void ignoreDependencyInterface(Class<?> ifc) {
        this.ignoredDependencyInterfaces.add(ifc);
    }

    @Override
    public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
        super.copyConfigurationFrom(otherFactory);
        if (otherFactory instanceof AbstractAutowireCapableBeanFactory) {
            AbstractAutowireCapableBeanFactory otherAutowireFactory =
                    (AbstractAutowireCapableBeanFactory) otherFactory;
            this.instantiationStrategy = otherAutowireFactory.instantiationStrategy;
            this.allowCircularReferences = otherAutowireFactory.allowCircularReferences;
            this.ignoredDependencyTypes.addAll(otherAutowireFactory.ignoredDependencyTypes);
            this.ignoredDependencyInterfaces.addAll(otherAutowireFactory.ignoredDependencyInterfaces);
        }
    }


    //-------------------------------------------------------------------------
    // Typical methods for creating and populating external bean instances
    //-------------------------------------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    //spring ioc提供给程序员的一个方法，spring ioc容器初始化完成后，我们可以调用这个方法创建一个外部bean, 这个方法可以完整运行spring实例化，初始化bean的流程
    //因为每次都是重新创建，所以这种方式只能创建原型bean
    public <T> T createBean(Class<T> beanClass) throws BeansException {
        // Use prototype bean definition, to avoid registering bean as dependent bean.
        RootBeanDefinition bd = new RootBeanDefinition(beanClass);
        bd.setScope(SCOPE_PROTOTYPE);
        bd.allowCaching = ClassUtils.isCacheSafe(beanClass, getBeanClassLoader());
        return (T) createBean(beanClass.getName(), bd, null);
    }

    @Override
    // 同上，可以为一个外部bean完成自动注入
    public void autowireBean(Object existingBean) {
        // Use non-singleton bean definition, to avoid registering bean as dependent bean.
        RootBeanDefinition bd = new RootBeanDefinition(ClassUtils.getUserClass(existingBean));
        bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
        bd.allowCaching = ClassUtils.isCacheSafe(bd.getBeanClass(), getBeanClassLoader());
        BeanWrapper bw = new BeanWrapperImpl(existingBean);
        initBeanWrapper(bw);
        populateBean(bd.getBeanClass().getName(), bd, bw);
    }

    @Override
    // 同上， 可以为一个外部bean进行配置，包括自动注入， 初始化
    public Object configureBean(Object existingBean, String beanName) throws BeansException {
        markBeanAsCreated(beanName);
        BeanDefinition mbd = getMergedBeanDefinition(beanName);
        RootBeanDefinition bd = null;
        if (mbd instanceof RootBeanDefinition) {
            RootBeanDefinition rbd = (RootBeanDefinition) mbd;
            bd = (rbd.isPrototype() ? rbd : rbd.cloneBeanDefinition());
        }
        if (bd == null) {
            bd = new RootBeanDefinition(mbd);
        }
        if (!bd.isPrototype()) {
            bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
            bd.allowCaching = ClassUtils.isCacheSafe(ClassUtils.getUserClass(existingBean), getBeanClassLoader());
        }
        BeanWrapper bw = new BeanWrapperImpl(existingBean);
        initBeanWrapper(bw);
        populateBean(beanName, bd, bw);
        return initializeBean(beanName, existingBean, bd);
    }

    @Override
    @Nullable
    public Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName) throws BeansException {
        return resolveDependency(descriptor, requestingBeanName, null, null);
    }


    //-------------------------------------------------------------------------
    // Specialized methods for fine-grained control over the bean lifecycle
    //-------------------------------------------------------------------------

    @Override
    public Object createBean(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException {
        // Use non-singleton bean definition, to avoid registering bean as dependent bean.
        RootBeanDefinition bd = new RootBeanDefinition(beanClass, autowireMode, dependencyCheck);
        bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
        return createBean(beanClass.getName(), bd, null);
    }

    @Override
    public Object autowire(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException {
        // Use non-singleton bean definition, to avoid registering bean as dependent bean.
        final RootBeanDefinition bd = new RootBeanDefinition(beanClass, autowireMode, dependencyCheck);
        bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
        if (bd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR) {
            return autowireConstructor(beanClass.getName(), bd, null, null).getWrappedInstance();
        }
        else {
            Object bean;
            final BeanFactory parent = this;
            if (System.getSecurityManager() != null) {
                bean = AccessController.doPrivileged((PrivilegedAction<Object>) () ->
                                getInstantiationStrategy().instantiate(bd, null, parent),
                        getAccessControlContext());
            }
            else {
                bean = getInstantiationStrategy().instantiate(bd, null, parent);
            }
            populateBean(beanClass.getName(), bd, new BeanWrapperImpl(bean));
            return bean;
        }
    }

    @Override
    public void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck)
            throws BeansException {

        if (autowireMode == AUTOWIRE_CONSTRUCTOR) {
            throw new IllegalArgumentException("AUTOWIRE_CONSTRUCTOR not supported for existing bean instance");
        }
        // Use non-singleton bean definition, to avoid registering bean as dependent bean.
        RootBeanDefinition bd =
                new RootBeanDefinition(ClassUtils.getUserClass(existingBean), autowireMode, dependencyCheck);
        bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
        BeanWrapper bw = new BeanWrapperImpl(existingBean);
        initBeanWrapper(bw);
        populateBean(bd.getBeanClass().getName(), bd, bw);
    }

    @Override
    public void applyBeanPropertyValues(Object existingBean, String beanName) throws BeansException {
        markBeanAsCreated(beanName);
        BeanDefinition bd = getMergedBeanDefinition(beanName);
        BeanWrapper bw = new BeanWrapperImpl(existingBean);
        initBeanWrapper(bw);
        applyPropertyValues(beanName, bd, bw, bd.getPropertyValues());
    }

    @Override
    public Object initializeBean(Object existingBean, String beanName) {
        return initializeBean(beanName, existingBean, null);
    }

    @Override
    public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
            throws BeansException {

        Object result = existingBean;
        for (BeanPostProcessor processor : getBeanPostProcessors()) {
            Object current = processor.postProcessBeforeInitialization(result, beanName);
            if (current == null) {
                return result;
            }
            result = current;
        }
        return result;
    }

    @Override
    public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
            throws BeansException {

        Object result = existingBean;
        for (BeanPostProcessor processor : getBeanPostProcessors()) {
            Object current = processor.postProcessAfterInitialization(result, beanName);
            if (current == null) {
                return result;
            }
            result = current;
        }
        return result;
    }

    @Override
    public void destroyBean(Object existingBean) {
        new DisposableBeanAdapter(existingBean, getBeanPostProcessors(), getAccessControlContext()).destroy();
    }


    //---------------------------------------------------------------------
    // Implementation of relevant AbstractBeanFactory template methods
    //---------------------------------------------------------------------

    /**
     * 这个类的中心方法：创建bean实例，填充属性， 应用postprocessors等等
     *
     * 通过resolveBeanClass解析BeanDefinition的class属性。
     * 处理override属性(处理 lookup-method 和 replace-method 配置，Spring 将这两个配置统称为 override method)。
     * 通过resolveBeforeInstantiation进行实例化的前置处理。
     * 最后通过doCreateBean创建bean对象。
     *
     * Central method of this class: creates a bean instance,
     * populates the bean instance, applies post-processors, etc.
     * @see #doCreateBean
     */
    @Override
    protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
            throws BeanCreationException {

        if (logger.isDebugEnabled()) {
            logger.debug("Creating instance of bean '" + beanName + "'");
        }
        RootBeanDefinition mbdToUse = mbd;

        // Make sure bean class is actually resolved at this point, and
        // clone the bean definition in case of a dynamically resolved Class
        // which cannot be stored in the shared merged bean definition.
        // 确保此时的bean已经被解析了
        Class<?> resolvedClass = resolveBeanClass(mbd, beanName);

        if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
            /**个人理解：
             * 使用mbdToUse来创建实例，mbdToUse是对mbd的一个拷贝，然后把mbd动态解析得到的class直接保存包mbdToUse的属性中，
             * 这样如果要使用class就直接从mbdToUse get就可以了，不用再次调用resolveBeanClass(mbd, beanName)得到
             */
            mbdToUse = new RootBeanDefinition(mbd);
            mbdToUse.setBeanClass(resolvedClass);
        }

        // Prepare method overrides.
        // 处理 lookup-method 和 replace-method 配置，Spring 将这两个配置统称为 override method
        try {
            mbdToUse.prepareMethodOverrides();
        }
        catch (BeanDefinitionValidationException ex) {
            throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),
                    beanName, "Validation of method overrides failed", ex);
        }

        try {
            /**
             * Spring的一个扩展点: InstantiationAwareBeanPostProcessor
             * 在 bean 创建前调用InstantiationAwareBeanPostProcessor(继承自BeanPostProcessor,在这里触发调用)后置处理器，如果后置处理返回的 bean 不为空，则直接返回
             * 如果我们不想让spring填充bean的依赖了，可以实现InstantiationAwareBeanPostProcessor，在这里回调，我们可以直接把bean new出来返回，不做任何处理。
             *
             * 如果开发人员自己提供了targetSource, 动态代理实例就在这里创建的，使用的就是AnnotationAwareAspectJAutoProxyCreator， 最终调用AbstractAutoProxyCreator#postProcessBeforeInstantiation
             */
            // Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
            Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
            if (bean != null) {
                return bean;
            }
        }
        catch (Throwable ex) {
            throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName,
                    "BeanPostProcessor before instantiation of bean failed", ex);
        }

        try {
            // 调用doCreateBean 创建bean实例
            Object beanInstance = doCreateBean(beanName, mbdToUse, args);
            if (logger.isDebugEnabled()) {
                logger.debug("Finished creating instance of bean '" + beanName + "'");
            }
            return beanInstance;
        }
        catch (BeanCreationException | ImplicitlyAppearedSingletonException ex) {
            // A previously detected exception with proper bean creation context already,
            // or illegal singleton state to be communicated up to DefaultSingletonBeanRegistry.
            throw ex;
        }
        catch (Throwable ex) {
            throw new BeanCreationException(
                    mbdToUse.getResourceDescription(), beanName, "Unexpected exception during bean creation", ex);
        }
    }

    /**
     * Actually create the specified bean. Pre-creation processing has already happened
     * at this point, e.g. checking {@code postProcessBeforeInstantiation} callbacks.
     * <p>Differentiates between default bean instantiation, use of a
     * factory method, and autowiring a constructor.
     * @param beanName the name of the bean
     * @param mbd the merged bean definition for the bean
     * @param args explicit arguments to use for constructor or factory method invocation
     * @return a new instance of the bean
     * @throws BeanCreationException if the bean could not be created
     * @see #instantiateBean
     * @see #instantiateUsingFactoryMethod
     * @see #autowireConstructor
     */
    protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final @Nullable Object[] args)
            throws BeanCreationException {
        // Instantiate the bean.

        // BeanWrapper是对Bean的包装，其接口中所定义的功能很简单包括设置获取被包装的对象、获取被包装bean的属性描述器
        BeanWrapper instanceWrapper = null;

        // 如果是单例模型，则从未完成的FactoryBean缓存factoryBeanInstanceCache中删除
        if (mbd.isSingleton()) {
            instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
        }
        if (instanceWrapper == null) {
            /**
             * 使用合适的实例化策略来创建 bean 实例，并将实例包裹在 BeanWrapper 实现类对象中返回。
             * createBeanInstance中包含三种创建 bean 实例的方式：
             *   1. 通过工厂方法创建 bean 实例 ：xml中指明的factory-method  @bean指明的也是工厂方法
             *   2. 通过构造方法自动注入（autowire by constructor）的方式创建 bean 实例
             *   3. 通过构造方法方法创建 bean 实例
             *
             *
             * 增强 bean 实例, 具体进入方法里面查看注释和调试 若 bean 的配置信息中配置了 lookup-method 和 replace-method，则会使用 CGLIB
             */
            instanceWrapper = createBeanInstance(beanName, mbd, args);
        }
        //这里得到的是原生对象，还没有经过后置处理器处理的，
        //直接使用construct.newInstance()方法实例化出来的原生对象
        final Object bean = instanceWrapper.getWrappedInstance();

        Class<?> beanType = instanceWrapper.getWrappedClass();
        if (beanType != NullBean.class) {
            mbd.resolvedTargetType = beanType;
        }

        // Allow post-processors to modify the merged bean definition.
        synchronized (mbd.postProcessingLock) {
            if (!mbd.postProcessed) {
                try {
                    /**
                     * 很重要的一个方法
                     * Spring框架同时提供了一个机会给开发人员或者框架其他部分提供了一个扩展点 ：
                     * 在使用mergedBeanDefinition实例化bean之后，bean属性填充之前，对该bean和该mergedBeanDefinition做一次回调，
                     * 相应的回调接口是MergedBeanDefinitionPostProcessor。
                     * public interface MergedBeanDefinitionPostProcessor extends BeanPostProcessor
                     */
                    applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
                }
                catch (Throwable ex) {
                    throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                            "Post-processing of merged bean definition failed", ex);
                }
                mbd.postProcessed = true;
            }
        }

        // Eagerly cache singletons to be able to resolve circular references
        // even when triggered by lifecycle interfaces like BeanFactoryAware.
        // 给三级缓存中的singletonFactories缓存添加一个<beanname,ObjectFactory>键值对 ： 解决循环依赖
        boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
                isSingletonCurrentlyInCreation(beanName));
        if (earlySingletonExposure) {
            if (logger.isDebugEnabled()) {
                logger.debug("Eagerly caching bean '" + beanName +
                        "' to allow for resolving potential circular references");
            }
            // getEarlyBeanReference实现如下：
            // if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
            //      SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
            //      exposedObject = ibp.getEarlyBeanReference(exposedObject, beanName);
            // }
            // SmartInstantiationAwareBeanPostProcessor#getEarlyBeanReference的实现有
            // InstantiationAwareBeanPostProcessorAdapter.getEarlyBeanReference  直接返回传进来的bean
            // @EnableAspectJAutoProxy会引入AbstractAutoProxyCreator这个SmartInstantiationAwareBeanPostProcessor
            // AbstractAutoProxyCreator.getEarlyBeanReference#wrapIfNecessary  判断是否需要创建代理，需要代理的话返回bean的代理对象，不需要的代理的话直接返回传进来的bean
            addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
        }

        // Initialize the bean instance.
        // 前面已经完成了实例化，现在开始填充bean的属性， 初始化等
        Object exposedObject = bean;
        try {
            //设置属性(添加依赖)，非常重要
            populateBean(beanName, mbd, instanceWrapper);
            //执行后置处理器before方法，生命周期回调方法初始化， 执行后置处理器after方法
            exposedObject = initializeBean(beanName, exposedObject, mbd);
        }
        catch (Throwable ex) {
            if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
                throw (BeanCreationException) ex;
            }
            else {
                throw new BeanCreationException(
                        mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
            }
        }

        // 进行依赖检查
        if (earlySingletonExposure) {
            Object earlySingletonReference = getSingleton(beanName, false);
            if (earlySingletonReference != null) {
                if (exposedObject == bean) {
                    exposedObject = earlySingletonReference;
                }
                else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
                    String[] dependentBeans = getDependentBeans(beanName);
                    Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
                    for (String dependentBean : dependentBeans) {
                        if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
                            actualDependentBeans.add(dependentBean);
                        }
                    }
                    if (!actualDependentBeans.isEmpty()) {
                        throw new BeanCurrentlyInCreationException(beanName,
                                "Bean with name '" + beanName + "' has been injected into other beans [" +
                                        StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
                                        "] in its raw version as part of a circular reference, but has eventually been " +
                                        "wrapped. This means that said other beans do not use the final version of the " +
                                        "bean. This is often the result of over-eager type matching - consider using " +
                                        "'getBeanNamesOfType' with the 'allowEagerInit' flag turned off, for example.");
                    }
                }
            }
        }

        // Register bean as disposable.
        /**
         *  给bean注册销毁时使用的对象:
         *  单例bean：disposableBeans.put(beanName, DisposableBeanAdapter)
         *  其他非原型作用域bean :　registerDestructionCallback(beanName, DisposableBeanAdapter)
         *  Spring不保留对其创建的原型Bean的任何引用
         *
         */
        try {
            registerDisposableBeanIfNecessary(beanName, bean, mbd);
        }
        catch (BeanDefinitionValidationException ex) {
            throw new BeanCreationException(
                    mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
        }

        return exposedObject;
    }

    @Override
    @Nullable
    protected Class<?> predictBeanType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
        Class<?> targetType = determineTargetType(beanName, mbd, typesToMatch);

        // Apply SmartInstantiationAwareBeanPostProcessors to predict the
        // eventual type after a before-instantiation shortcut.
        if (targetType != null && !mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
            for (BeanPostProcessor bp : getBeanPostProcessors()) {
                if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
                    SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
                    Class<?> predicted = ibp.predictBeanType(targetType, beanName);
                    if (predicted != null && (typesToMatch.length != 1 || FactoryBean.class != typesToMatch[0] ||
                            FactoryBean.class.isAssignableFrom(predicted))) {
                        return predicted;
                    }
                }
            }
        }
        return targetType;
    }

    /**
     * Determine the target type for the given bean definition.
     * @param beanName the name of the bean (for error handling purposes)
     * @param mbd the merged bean definition for the bean
     * @param typesToMatch the types to match in case of internal type matching purposes
     * (also signals that the returned {@code Class} will never be exposed to application code)
     * @return the type for the bean if determinable, or {@code null} otherwise
     */
    @Nullable
    protected Class<?> determineTargetType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
        Class<?> targetType = mbd.getTargetType();
        if (targetType == null) {
            targetType = (mbd.getFactoryMethodName() != null ?
                    getTypeForFactoryMethod(beanName, mbd, typesToMatch) :
                    resolveBeanClass(mbd, beanName, typesToMatch));
            if (ObjectUtils.isEmpty(typesToMatch) || getTempClassLoader() == null) {
                mbd.resolvedTargetType = targetType;
            }
        }
        return targetType;
    }

    /**
     * Determine the target type for the given bean definition which is based on
     * a factory method. Only called if there is no singleton instance registered
     * for the target bean already.
     * <p>This implementation determines the type matching {@link #createBean}'s
     * different creation strategies. As far as possible, we'll perform static
     * type checking to avoid creation of the target bean.
     * @param beanName the name of the bean (for error handling purposes)
     * @param mbd the merged bean definition for the bean
     * @param typesToMatch the types to match in case of internal type matching purposes
     * (also signals that the returned {@code Class} will never be exposed to application code)
     * @return the type for the bean if determinable, or {@code null} otherwise
     * @see #createBean
     */
    @Nullable
    //解析工厂方法构造的bean的类型
    //(包括静态工厂方法， 动态工厂方法， 静态@bean method作为工厂方法， @bean method作为工厂方法)
    protected Class<?> getTypeForFactoryMethod(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
        ResolvableType cachedReturnType = mbd.factoryMethodReturnType;
        if (cachedReturnType != null) {
            //如果已经缓存了mbd.factoryMethodReturnType， 那么直接返回
            return cachedReturnType.resolve();
        }

        Class<?> factoryClass;
        boolean isStatic = true;

        String factoryBeanName = mbd.getFactoryBeanName();
        if (factoryBeanName != null) {
            if (factoryBeanName.equals(beanName)) {
                throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName,
                        "factory-bean reference points back to the same bean definition");
            }
            // Check declared factory method return type on factory class.
            factoryClass = getType(factoryBeanName);
            isStatic = false;
        }
        else {
            // Check declared factory method return type on bean class.
            factoryClass = resolveBeanClass(mbd, beanName, typesToMatch);
        }

        if (factoryClass == null) {
            return null;
        }
        factoryClass = ClassUtils.getUserClass(factoryClass);

        // If all factory methods have the same return type, return that type.
        // Can't clearly figure out exact method due to type converting / autowiring!
        Class<?> commonType = null;
        Method uniqueCandidate = null;
        int minNrOfArgs =
                (mbd.hasConstructorArgumentValues() ? mbd.getConstructorArgumentValues().getArgumentCount() : 0);
        Method[] candidates = ReflectionUtils.getUniqueDeclaredMethods(factoryClass);
        for (Method candidate : candidates) {
            if (Modifier.isStatic(candidate.getModifiers()) == isStatic && mbd.isFactoryMethod(candidate) &&
                    candidate.getParameterCount() >= minNrOfArgs) {
                // Declared type variables to inspect?
                if (candidate.getTypeParameters().length > 0) {
                    try {
                        // Fully resolve parameter names and argument values.
                        Class<?>[] paramTypes = candidate.getParameterTypes();
                        String[] paramNames = null;
                        ParameterNameDiscoverer pnd = getParameterNameDiscoverer();
                        if (pnd != null) {
                            paramNames = pnd.getParameterNames(candidate);
                        }
                        ConstructorArgumentValues cav = mbd.getConstructorArgumentValues();
                        Set<ConstructorArgumentValues.ValueHolder> usedValueHolders = new HashSet<>(paramTypes.length);
                        Object[] args = new Object[paramTypes.length];
                        for (int i = 0; i < args.length; i++) {
                            ConstructorArgumentValues.ValueHolder valueHolder = cav.getArgumentValue(
                                    i, paramTypes[i], (paramNames != null ? paramNames[i] : null), usedValueHolders);
                            if (valueHolder == null) {
                                valueHolder = cav.getGenericArgumentValue(null, null, usedValueHolders);
                            }
                            if (valueHolder != null) {
                                args[i] = valueHolder.getValue();
                                usedValueHolders.add(valueHolder);
                            }
                        }
                        Class<?> returnType = AutowireUtils.resolveReturnTypeForFactoryMethod(
                                candidate, args, getBeanClassLoader());
                        uniqueCandidate = (commonType == null && returnType == candidate.getReturnType() ?
                                candidate : null);
                        commonType = ClassUtils.determineCommonAncestor(returnType, commonType);
                        if (commonType == null) {
                            // Ambiguous return types found: return null to indicate "not determinable".
                            return null;
                        }
                    }
                    catch (Throwable ex) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Failed to resolve generic return type for factory method: " + ex);
                        }
                    }
                }
                else {
                    uniqueCandidate = (commonType == null ? candidate : null);
                    commonType = ClassUtils.determineCommonAncestor(candidate.getReturnType(), commonType);
                    if (commonType == null) {
                        // Ambiguous return types found: return null to indicate "not determinable".
                        return null;
                    }
                }
            }
        }

        if (commonType == null) {
            return null;
        }
        // Common return type found: all factory methods return same type. For a non-parameterized
        // unique candidate, cache the full type declaration context of the target factory method.
        cachedReturnType = (uniqueCandidate != null ?
                ResolvableType.forMethodReturnType(uniqueCandidate) : ResolvableType.forClass(commonType));
        mbd.factoryMethodReturnType = cachedReturnType;
        return cachedReturnType.resolve();
    }

    /**
     * This implementation attempts to query the FactoryBean's generic parameter metadata
     * if present to determine the object type. If not present, i.e. the FactoryBean is
     * declared as a raw type, checks the FactoryBean's {@code getObjectType} method
     * on a plain instance of the FactoryBean, without bean properties applied yet.
     * If this doesn't return a type yet, a full creation of the FactoryBean is
     * used as fallback (through delegation to the superclass's implementation).
     * <p>The shortcut check for a FactoryBean is only applied in case of a singleton
     * FactoryBean. If the FactoryBean instance itself is not kept as singleton,
     * it will be fully created to check the type of its exposed object.
     */
    @Override
    @Nullable
    protected Class<?> getTypeForFactoryBean(String beanName, RootBeanDefinition mbd) {
        String factoryBeanName = mbd.getFactoryBeanName();
        String factoryMethodName = mbd.getFactoryMethodName();

        if (factoryBeanName != null) {
            if (factoryMethodName != null) {
                // Try to obtain the FactoryBean's object type from its factory method declaration
                // without instantiating the containing bean at all.
                BeanDefinition fbDef = getBeanDefinition(factoryBeanName);
                if (fbDef instanceof AbstractBeanDefinition) {
                    AbstractBeanDefinition afbDef = (AbstractBeanDefinition) fbDef;
                    if (afbDef.hasBeanClass()) {
                        Class<?> result = getTypeForFactoryBeanFromMethod(afbDef.getBeanClass(), factoryMethodName);
                        if (result != null) {
                            return result;
                        }
                    }
                }
            }
            // If not resolvable above and the referenced factory bean doesn't exist yet,
            // exit here - we don't want to force the creation of another bean just to
            // obtain a FactoryBean's object type...
            if (!isBeanEligibleForMetadataCaching(factoryBeanName)) {
                return null;
            }
        }

        // Let's obtain a shortcut instance for an early getObjectType() call...
        FactoryBean<?> fb = (mbd.isSingleton() ?
                getSingletonFactoryBeanForTypeCheck(beanName, mbd) :
                getNonSingletonFactoryBeanForTypeCheck(beanName, mbd));

        if (fb != null) {
            // Try to obtain the FactoryBean's object type from this early stage of the instance.
            Class<?> result = getTypeForFactoryBean(fb);
            if (result != null) {
                return result;
            }
            else {
                // No type found for shortcut FactoryBean instance:
                // fall back to full creation of the FactoryBean instance.
                return super.getTypeForFactoryBean(beanName, mbd);
            }
        }

        if (factoryBeanName == null && mbd.hasBeanClass()) {
            // No early bean instantiation possible: determine FactoryBean's type from
            // static factory method signature or from class inheritance hierarchy...
            if (factoryMethodName != null) {
                return getTypeForFactoryBeanFromMethod(mbd.getBeanClass(), factoryMethodName);
            }
            else {
                return GenericTypeResolver.resolveTypeArgument(mbd.getBeanClass(), FactoryBean.class);
            }
        }

        return null;
    }

    /**
     * Introspect the factory method signatures on the given bean class,
     * trying to find a common {@code FactoryBean} object type declared there.
     * @param beanClass the bean class to find the factory method on
     * @param factoryMethodName the name of the factory method
     * @return the common {@code FactoryBean} object type, or {@code null} if none
     */
    @Nullable
    private Class<?> getTypeForFactoryBeanFromMethod(Class<?> beanClass, final String factoryMethodName) {
        class Holder { @Nullable Class<?> value = null; }
        final Holder objectType = new Holder();

        // CGLIB subclass methods hide generic parameters; look at the original user class.
        Class<?> fbClass = ClassUtils.getUserClass(beanClass);

        // Find the given factory method, taking into account that in the case of
        // @Bean methods, there may be parameters present.
        ReflectionUtils.doWithMethods(fbClass, method -> {
            if (method.getName().equals(factoryMethodName) &&
                    FactoryBean.class.isAssignableFrom(method.getReturnType())) {
                Class<?> currentType = GenericTypeResolver.resolveReturnTypeArgument(method, FactoryBean.class);
                if (currentType != null) {
                    objectType.value = ClassUtils.determineCommonAncestor(currentType, objectType.value);
                }
            }
        });

        return (objectType.value != null && Object.class != objectType.value ? objectType.value : null);
    }

    /**
     * Obtain a reference for early access to the specified bean,
     * typically for the purpose of resolving a circular reference.
     * @param beanName the name of the bean (for error handling purposes)
     * @param mbd the merged bean definition for the bean
     * @param bean the raw bean instance
     * @return the object to expose as bean reference
     */
    protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
        Object exposedObject = bean;
        if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
            for (BeanPostProcessor bp : getBeanPostProcessors()) {
                if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
                    SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
                    exposedObject = ibp.getEarlyBeanReference(exposedObject, beanName);
                }
            }
        }
        return exposedObject;
    }


    //---------------------------------------------------------------------
    // Implementation methods
    //---------------------------------------------------------------------

    /**
     * Obtain a "shortcut" singleton FactoryBean instance to use for a
     * {@code getObjectType()} call, without full initialization of the FactoryBean.
     * @param beanName the name of the bean
     * @param mbd the bean definition for the bean
     * @return the FactoryBean instance, or {@code null} to indicate
     * that we couldn't obtain a shortcut FactoryBean instance
     */
    @Nullable
    private FactoryBean<?> getSingletonFactoryBeanForTypeCheck(String beanName, RootBeanDefinition mbd) {
        synchronized (getSingletonMutex()) {
            BeanWrapper bw = this.factoryBeanInstanceCache.get(beanName);
            if (bw != null) {
                return (FactoryBean<?>) bw.getWrappedInstance();
            }
            Object beanInstance = getSingleton(beanName, false);
            if (beanInstance instanceof FactoryBean) {
                return (FactoryBean<?>) beanInstance;
            }
            if (isSingletonCurrentlyInCreation(beanName) ||
                    (mbd.getFactoryBeanName() != null && isSingletonCurrentlyInCreation(mbd.getFactoryBeanName()))) {
                return null;
            }

            Object instance;
            try {
                // Mark this bean as currently in creation, even if just partially.
                beforeSingletonCreation(beanName);
                // Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
                instance = resolveBeforeInstantiation(beanName, mbd);
                if (instance == null) {
                    bw = createBeanInstance(beanName, mbd, null);
                    instance = bw.getWrappedInstance();
                }
            }
            finally {
                // Finished partial creation of this bean.
                afterSingletonCreation(beanName);
            }

            FactoryBean<?> fb = getFactoryBean(beanName, instance);
            if (bw != null) {
                this.factoryBeanInstanceCache.put(beanName, bw);
            }
            return fb;
        }
    }

    /**
     * Obtain a "shortcut" non-singleton FactoryBean instance to use for a
     * {@code getObjectType()} call, without full initialization of the FactoryBean.
     * @param beanName the name of the bean
     * @param mbd the bean definition for the bean
     * @return the FactoryBean instance, or {@code null} to indicate
     * that we couldn't obtain a shortcut FactoryBean instance
     */
    @Nullable
    private FactoryBean<?> getNonSingletonFactoryBeanForTypeCheck(String beanName, RootBeanDefinition mbd) {
        if (isPrototypeCurrentlyInCreation(beanName)) {
            return null;
        }

        Object instance = null;
        try {
            // Mark this bean as currently in creation, even if just partially.
            beforePrototypeCreation(beanName);
            // Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
            instance = resolveBeforeInstantiation(beanName, mbd);
            if (instance == null) {
                BeanWrapper bw = createBeanInstance(beanName, mbd, null);
                instance = bw.getWrappedInstance();
            }
        }
        catch (BeanCreationException ex) {
            // Can only happen when getting a FactoryBean.
            if (logger.isDebugEnabled()) {
                logger.debug("Bean creation exception on non-singleton FactoryBean type check: " + ex);
            }
            onSuppressedException(ex);
            return null;
        }
        finally {
            // Finished partial creation of this bean.
            afterPrototypeCreation(beanName);
        }

        return getFactoryBean(beanName, instance);
    }

    /**
     * Apply MergedBeanDefinitionPostProcessors to the specified bean definition,
     * invoking their {@code postProcessMergedBeanDefinition} methods.
     * @param mbd the merged bean definition for the bean
     * @param beanType the actual type of the managed bean instance
     * @param beanName the name of the bean
     * @see MergedBeanDefinitionPostProcessor#postProcessMergedBeanDefinition
     */
    protected void applyMergedBeanDefinitionPostProcessors(RootBeanDefinition mbd, Class<?> beanType, String beanName) {
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof MergedBeanDefinitionPostProcessor) {
                MergedBeanDefinitionPostProcessor bdp = (MergedBeanDefinitionPostProcessor) bp;
                bdp.postProcessMergedBeanDefinition(mbd, beanType, beanName);
            }
        }
    }

    /**
     * Apply before-instantiation post-processors, resolving whether there is a
     * before-instantiation shortcut for the specified bean.
     * @param beanName the name of the bean
     * @param mbd the bean definition for the bean
     * @return the shortcut-determined bean instance, or {@code null} if none
     */
    @Nullable
    protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
        Object bean = null;
        if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {
            // Make sure bean class is actually resolved at this point.
            if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
                Class<?> targetType = determineTargetType(beanName, mbd);
                if (targetType != null) {
                    bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
                    if (bean != null) {
                        bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
                    }
                }
            }
            mbd.beforeInstantiationResolved = (bean != null);
        }
        return bean;
    }

    /**
     * Apply InstantiationAwareBeanPostProcessors to the specified bean definition
     * (by class and name), invoking their {@code postProcessBeforeInstantiation} methods.
     * <p>Any returned object will be used as the bean instead of actually instantiating
     * the target bean. A {@code null} return value from the post-processor will
     * result in the target bean being instantiated.
     * @param beanClass the class of the bean to be instantiated
     * @param beanName the name of the bean
     * @return the bean object to use instead of a default instance of the target bean, or {@code null}
     * @see InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation
     */
    @Nullable
    protected Object applyBeanPostProcessorsBeforeInstantiation(Class<?> beanClass, String beanName) {
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof InstantiationAwareBeanPostProcessor) {
                InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                Object result = ibp.postProcessBeforeInstantiation(beanClass, beanName);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * 使用对应的方式实例化指定的beanName :
     * 提供了Supplier扩展点实例化、工厂方法的实例化 、普通@Component的实例化(有参构造函数实例化 无参构造函数实例化)
     * 、@bean method实例化、lookup-method/replaced-method实例化
     *
     * 工厂方法只有xml，没有对应注解，如下：
     * <bean id="bmwCar" class="com.home.factoryMethod.CarStaticFactory" factory-method="getCar">
     *
     * Create a new instance for the specified bean, using an appropriate instantiation strategy:
     * factory method, constructor autowiring, or simple instantiation.
     * @param beanName the name of the bean
     * @param mbd the bean definition for the bean
     * @param args explicit arguments to use for constructor or factory method invocation
     * @return a BeanWrapper for the new instance
     * @see #obtainFromSupplier
     * @see #instantiateUsingFactoryMethod
     * @see #autowireConstructor
     * @see #instantiateBean
     */
    protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) {
        // Make sure bean class is actually resolved at this point.
        // 待验证点 ：如果是动态代理，这里得到的beanClass应该是增强后的Class
        Class<?> beanClass = resolveBeanClass(mbd, beanName);

        /**
         * 检测一个类的访问权限spring默认情况下对于非public的类是允许访问的。
         * 确保class不为空，并且访问权限为public
         */
        if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers()) && !mbd.isNonPublicAccessAllowed()) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                    "Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
        }

        //==================================第一种实例化  begin ===============================================================
        //实例化过程中的一个扩展点：
        //如果存在supplier回调，则使用给定的回调方法实例化策略
        //如果有注册Supplier接口给bd的话，直接从给定的供应商(Supplier接口的实现类)获取bean实例：createBeanInstance->obtainFromSupplier
        Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
        if (instanceSupplier != null) {
            return obtainFromSupplier(instanceSupplier, beanName);
        }
        //==================================第一种实例化  end  =================================================================


        //==================================第二种实例化  begin ===============================================================
        /**
         * 如果工厂方法factory-method不为空，则通过工厂方法构建 bean 对象
         * <bean id="bmwCar" class="com.home.factoryMethod.CarStaticFactory" factory-method="getCar">
         *
         * 如果是@bean方式，也是在这里实例化：
         * 使用下面这种方式BeanMethodPVS对应的bd类型为ConfigurationClassBeanDefinitionReader$ConfigurationClassBeanDefinition, factory-method的名字为beanMethodPVS
         * 这个工厂方法是配置类的方法，spring中配置类的实例是cglib增强后的实力，就是对这个工厂方法进行了增强，增加了ConfigurationClassEnhancer.BeanMethodInterceptor#intercept()这个切面逻辑
         * @Bean
         * public BeanMethodPVS beanMethodPVS(){
         *     return new BeanMethodPVS();
         * }
         *
         */
        if (mbd.getFactoryMethodName() != null)  {
            //委托给ConstructorResolver进行处理： ConstructorResolver(this).instantiateUsingFactoryMethod
            return instantiateUsingFactoryMethod(beanName, mbd, args);
        }
        //==================================第二种实例化  end =================================================================

        //================================== 第二次及以后实例化时可以直接使用mbd中保存的resolvedConstructorOrFactoryMethod和constructorArgumentsResolved  begin===========
        // Shortcut when re-creating the same bean...
        /**
         * 从spring的原始注释可以知道这个是一个Shortcut，什么意思呢？
         * 当多次构建同一个 bean 时，可以使用这个Shortcut，也就是说不在需要次推断应该使用哪种方式构造bean
         * 比如在多次构建同一个prototype类型的 bean 时，就可以走此处的shortcut
         * resolvedConstructorOrFactoryMethod 和 mbd.constructorArgumentsResolved 将会在 bean 第一次实例化的过程中被设置，后面来证明
         */
        boolean resolved = false;  //一个标志，如果mbd.resolvedConstructorOrFactoryMethod不为null就会被设置为true,
        boolean autowireNecessary = false; //是否需要自动装配，构造器有参数的需要
        if (args == null) {
            synchronized (mbd.constructorArgumentLock) {
                if (mbd.resolvedConstructorOrFactoryMethod != null) {
                    resolved = true;
                    //如果已经解析了构造方法的参数，则必须要通过一个带参构造方法来实例
                    autowireNecessary = mbd.constructorArgumentsResolved;
                }
            }
        }
        if (resolved) {
            //构造器有参数的调用autowireConstructor进行实例化
            if (autowireNecessary) {
                // 通过构造方法自动装配的方式构造 bean 对象：
                // @Autowired注解在构造器上的方式，就是autowireConstructor这个方法对构造器的参数进行实例化
                // populateBean() 方法则是对@Autowired注解在属性的情况进行处理，处理的是AbstractBeanDefinition.propertyValues

                //委托给ConstructorResolver进行处理： ConstructorResolver(this).autowireConstructor
                return autowireConstructor(beanName, mbd, null, null);
            }
            //构造器没有参数的/lookup-method和replaced-method，调用instantiateBean进行实例化
            else {
                return instantiateBean(beanName, mbd);
            }
        }
        //================================== 第二次及以后实例化时可以直接使用mbd中保存的resolvedConstructorOrFactoryMethod和constructorArgumentsResolved  end=============

        //================================== 第一次实例化使用后置处理器SmartInstantiationAwareBeanPostProcessor的子类得到一组带参数的构造函数，然后调用autowireConstructor实例化  begin===========
        // Candidate constructors for autowiring?
        // 这个也是一个扩展点，可以使用这个后置处理器决定一组有参构造函数候选者
        // 目前真正实现determineConstructorsFromBeanPostProcessors这个方法的只有spring自己提供的AutowiredAnnotationBeanPostProcessor
        Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
        if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
                mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args))  {
            //委托给ConstructorResolver进行处理： ConstructorResolver(this).autowireConstructor
            return autowireConstructor(beanName, mbd, ctors, args);
        }
        //================================== 第一次实例化使用后置处理器SmartInstantiationAwareBeanPostProcessor的子类得到一组带参数的构造函数，然后调用autowireConstructor实例化  end=============


        // ========================最后使用默认的无参构造函数进行兜底实例化 begin ========================
        // No special handling: simply use no-arg constructor.
        // 最后进行兜底，使用默认无参构造方法实例化(构造器没有参数的/lookup-method和replaced-method都是用instantiateBean()这个方法进行实例化)
        return instantiateBean(beanName, mbd);
        // ========================最后使用默认的无参构造函数进行兜底实例化 end ==========================
    }

    /**
     * @param instanceSupplier 配置的supplier
     * @param beanName         对应的bean名称
     * @return 新实例的BeanWrapper
     * @see #getObjectForBeanInstance
     * @since 5.0
     */
    //  如果有注册Supplier接口给bd的话，直接从给定的供应商(Supplier接口的实现类)获取bean实例：createBeanInstance->obtainFromSupplier
    //  参考Supplier接口
    protected BeanWrapper obtainFromSupplier(Supplier<?> instanceSupplier, String beanName) {
        // 当前创建的bean的名称NamedThreadLocal<String> currentlyCreatedBean
        // 获取原先创建bean的名字
        String outerBean = this.currentlyCreatedBean.get();
        this.currentlyCreatedBean.set(beanName);
        Object instance;
        try {
            // 调用Supplier的get(),返回一个Bean对象。有点像工厂模式
            instance = instanceSupplier.get();
        }
        finally {
            if (outerBean != null) {
                //如果原先bean存在,将保存到currentlyCreatedBean中
                this.currentlyCreatedBean.set(outerBean);
            }
            else {
                this.currentlyCreatedBean.remove();
            }
        }
        //组装BeanWrapper
        BeanWrapper bw = new BeanWrapperImpl(instance);
        //初始化BeanWrapper
        return bw;
    }

    /**
     * Overridden in order to implicitly register the currently created bean as dependent on further beans getting programmatically retrieved during a {@link Supplier} callback.
     * @since 5.0
     * @see #obtainFromSupplier
     */
    @Override
    protected Object getObjectForBeanInstance(
            Object beanInstance, String name, String beanName, @Nullable RootBeanDefinition mbd) {

        String currentlyCreatedBean = this.currentlyCreatedBean.get();
        if (currentlyCreatedBean != null) {
            registerDependentBean(beanName, currentlyCreatedBean);
        }

        return super.getObjectForBeanInstance(beanInstance, name, beanName, mbd);
    }

    /**
     * Determine candidate constructors to use for the given bean, checking all registered
     * {@link SmartInstantiationAwareBeanPostProcessor SmartInstantiationAwareBeanPostProcessors}.
     * @param beanClass the raw class of the bean
     * @param beanName the name of the bean
     * @return the candidate constructors, or {@code null} if none specified
     * @throws org.springframework.beans.BeansException in case of errors
     * @see org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor#determineCandidateConstructors
     */
    @Nullable
    //确定要为给定bean使用的候选构造函数，检查所有已注册的构造函数
    protected Constructor<?>[] determineConstructorsFromBeanPostProcessors(@Nullable Class<?> beanClass, String beanName)
            throws BeansException {

        if (beanClass != null && hasInstantiationAwareBeanPostProcessors()) {
            for (BeanPostProcessor bp : getBeanPostProcessors()) {
                if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
                    SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
                    //真正实现这个方法的只有AutowiredAnnotationBeanPostProcessor.determineCandidateConstructors
                    Constructor<?>[] ctors = ibp.determineCandidateConstructors(beanClass, beanName);
                    if (ctors != null) {
                        return ctors;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Instantiate the given bean using its default constructor.
     *
     * @param beanName the name of the bean
     * @param mbd the bean definition for the bean
     * @return a BeanWrapper for the new instance
     */
    // 1. 无参的构造函数会调用这个方法进行实例化
    // 2. lookup-method  replaced-method也是调用这个方法，通过得到CglibSubclassingInstantiationStrategy这个策略对象，然后调用它的instantiate()方法进行实例化
    protected BeanWrapper instantiateBean(final String beanName, final RootBeanDefinition mbd) {
        try {
            Object beanInstance;
            final BeanFactory parent = this;
            if (System.getSecurityManager() != null) {
                beanInstance = AccessController.doPrivileged((PrivilegedAction<Object>) () ->
                                getInstantiationStrategy().instantiate(mbd, beanName, parent),
                        getAccessControlContext());
            }
            else {
                //getInstantiationStrategy()得到类的实例化策略
                //默认情况下是得到一个实例化策略进行实例化
                beanInstance = getInstantiationStrategy().instantiate(mbd, beanName, parent);
            }
            BeanWrapper bw = new BeanWrapperImpl(beanInstance);
            initBeanWrapper(bw);
            return bw;
        }
        catch (Throwable ex) {
            throw new BeanCreationException(
                    mbd.getResourceDescription(), beanName, "Instantiation of bean failed", ex);
        }
    }

    /**
     * Instantiate the bean using a named factory method. The method may be static, if the
     * mbd parameter specifies a class, rather than a factoryBean, or an instance variable
     * on a factory object itself configured using Dependency Injection.
     * @param beanName the name of the bean
     * @param mbd the bean definition for the bean
     * @param explicitArgs argument values passed in programmatically via the getBean method,
     * or {@code null} if none (-> use constructor argument values from bean definition)
     * @return a BeanWrapper for the new instance
     * @see #getBean(String, Object[])
     */
    //具体查看调用链就明白了
    protected BeanWrapper instantiateUsingFactoryMethod(
            String beanName, RootBeanDefinition mbd, @Nullable Object[] explicitArgs) {

        return new ConstructorResolver(this).instantiateUsingFactoryMethod(beanName, mbd, explicitArgs);
    }

    /**
     * "autowire constructor" (with constructor arguments by type) behavior.
     * Also applied if explicit constructor argument values are specified,
     * matching all remaining arguments with beans from the bean factory.
     * <p>This corresponds to constructor injection: In this mode, a Spring
     * bean factory is able to host components that expect constructor-based
     * dependency resolution.
     * @param beanName the name of the bean
     * @param mbd the bean definition for the bean
     * @param ctors the chosen candidate constructors
     * @param explicitArgs argument values passed in programmatically via the getBean method,
     * or {@code null} if none (-> use constructor argument values from bean definition)
     *
     * @return a BeanWrapper for the new instance
     */
    // “自动装配构造函数”(构造函数参数按类型)行为。
    //  构造函数需要的参数数组Object[]，就从bean definition中查找参数值(xml中配置的 或者 手工给bd设置的)， 没有的话通过spring容器getBean方法得到的
    // 入参：
    // beanName ： beanName
    // mbd ：mbd
    // ctors ：候选的构造函数列表
    // explicitArgs ： 构造函数需要的参数数组Object[]，就从bean definition中查找参数值(xml中配置的 或者 手工给bd设置的)， 没有的话通过spring容器getBean方法得到的
    protected BeanWrapper autowireConstructor(
            String beanName, RootBeanDefinition mbd, @Nullable Constructor<?>[] ctors, @Nullable Object[] explicitArgs) {

        return new ConstructorResolver(this).autowireConstructor(beanName, mbd, ctors, explicitArgs);
    }

    /**
     * Populate the bean instance in the given BeanWrapper with the property values
     * from the bean definition.
     * @param beanName the name of the bean
     * @param mbd the bean definition for the bean
     * @param bw the BeanWrapper with bean instance
     */
    /* 下面是依赖注入过程中用到的一些对象：
     * A) PropertyDescriptor：一个PropertyDescriptor对象描述Java Bean的一个属性，以及getter等访问器获取这个属性
     * B) InjectionElement：这个是注入元素，包含了注入元素的java.lang.reflect.Member 的对象，以及一个PropertyDescriptor对象。
     * 就是对java.lang.reflect.Member的一个封装，用来执行最终的注入动作，它有两个子类，分别是：AutowiredFieldElement表示字段属性，
     * AutowiredMethodElement表示方法。
     * C) InjectionMetadata：这个是注入元数据，包含了目标Bean的Class对象，和注入元素（InjectionElement）集合.
     * D) PropertyValue：这是一个用来表示Bean属性的对象，其中定义了属性的名字和 ！值！ 等信息，如simpleService，和simpleDao属性。
     * E) PropertyValues：里面包含多个PropertyValue
     */
    protected void populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw) {
        //bean实例创建出来才会到populateBean， 所以bw不能为null
        if (bw == null) {
            //bw为null, bd中还设置了属性，直接抛出异常
            if (mbd.hasPropertyValues()) {
                throw new BeanCreationException(
                        mbd.getResourceDescription(), beanName, "Cannot apply property values to null instance");
            }
            else {
                // Skip property population phase for null instance.
                return;
            }
        }

        // Give any InstantiationAwareBeanPostProcessors the opportunity to modify the
        // state of the bean before properties are set. This can be used, for example,
        // to support styles of field injection.
        boolean continueWithPropertyPopulation = true;

        if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
            //AbastractBeanFactory 中的 beanPostProcessors属性，然后判断是否bp instanceof InstantiationAwareBeanPostProcessor，是的话执行postProcessAfterInstantiation
            for (BeanPostProcessor bp : getBeanPostProcessors()) {
                if (bp instanceof InstantiationAwareBeanPostProcessor) {
                    InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                    if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
                        continueWithPropertyPopulation = false;
                        break;
                    }
                }
            }
        }

        if (!continueWithPropertyPopulation) {
            return;
        }

        //======================1 begin ==========================
        //我们可以使用bd对象设置构造方法的参数值，也可以设置属性值, spring内部的一些bean在这里之前就设置了属性值，然后在这里就可以获取到
        //不过我们自己定义的bean,在这里获取时都是null, 然后在2里面设置pvs
        PropertyValues pvs = (mbd.hasPropertyValues() ? mbd.getPropertyValues() : null);
        //======================1 end  ===========================

        //======================2 begin ==========================
        //使用xml配置的方式设置<beans default-autowire="byName/byType"/>的情况下会走这个分支获取pvs,然后在4中给bean的属性真正赋值
        //spring内部的一些bean构造bd的时候会把AbstractBeanDefinition.autowireMode设置为AUTOWIRE_BY_NAME 或者 AUTOWIRE_BY_TYPE
        //目前默认使用的@Autowired的注解方式，autowireMode都是默认值0，都不会走这个分支给bean的属性赋值，而是使用3这个分支
        if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_NAME || mbd.getResolvedAutowireMode() == AUTOWIRE_BY_TYPE) {
            MutablePropertyValues newPvs = new MutablePropertyValues(pvs);
            // Add property values based on autowire by name if applicable.
            if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_NAME) {
                autowireByName(beanName, mbd, bw, newPvs);
            }
            // Add property values based on autowire by type if applicable.
            if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_TYPE) {
                autowireByType(beanName, mbd, bw, newPvs);
            }
            pvs = newPvs;
        }
        //======================2 end  ===========================

        //======================3 begin ==========================
        //目前默认使用的@Autowired的注解方式，会走这个分支给bean的属性赋值
        //AutowiredAnnotationBeanPostProcessor.postProcessPropertyValues完成bean属性的注入
        boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();
        //AbstractBeanDefinition.DEPENDENCY_CHECK_NONE参考dingyi定义处的注释
        boolean needsDepCheck = (mbd.getDependencyCheck() != AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);

        if (hasInstAwareBpps || needsDepCheck) {
            if (pvs == null) {
                pvs = mbd.getPropertyValues();
            }
            //获取bean中get方法描述的属性
            PropertyDescriptor[] filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);

            if (hasInstAwareBpps) {
                //ConfigurationClassPostProcessor$ImportAwareBeanPostProcessor:之前讲过，看之前的笔记
                //CommonAnnotationBeanPostProcessor 主要处理@Resource、@PostConstruct和@PreDestroy注解的实现
                //AutowiredAnnotationBeanPostProcessor 在这个后置处理器中完成bean属性的注入
                for (BeanPostProcessor bp : getBeanPostProcessors()) {
                    if (bp instanceof InstantiationAwareBeanPostProcessor) {
                        InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                        pvs = ibp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);
                        if (pvs == null) {
                            return;
                        }
                    }
                }
            }
            if (needsDepCheck) {
                checkDependencies(beanName, mbd, filteredPds, pvs);
            }
        }
        //======================3 end  ===========================

        //======================4 begin ==========================
        if (pvs != null) {
            //给属性赋值(依赖注入)
            //pvs里面保存着这个bean的属性名和属性值(没有经过格式转换的)，bw里面保存PropertyEditor(每次都会重新new PropertyEditor)
            //在这个方法里面使用PropertyEditor转换bean的属性值的格式
            applyPropertyValues(beanName, mbd, bw, pvs);
        }
        //======================4 end  ===========================
    }

    /**
     * Fill in any missing property values with references to
     * other beans in this factory if autowire is set to "byName".
     * @param beanName the name of the bean we're wiring up.
     * Useful for debugging messages; not used functionally.
     * @param mbd bean definition to update through autowiring
     * @param bw the BeanWrapper from which we can obtain information about the bean
     * @param pvs the PropertyValues to register wired objects with
     */
    //在不设置<beans default-autowire="byName/byType"/>的情况下是不会调用这个方法的，如果设置了byName，调用autowireByName
    protected void autowireByName(
            String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {

        // 找到还没赋值的属性名称
        String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
        for (String propertyName : propertyNames) {
            if (containsBean(propertyName)) {
                // 递归调用getBean，如果没有创建并注册，有了直接返回
                Object bean = getBean(propertyName);
                // 将刚得到或创建的bean赋值给PropertyValue
                pvs.add(propertyName, bean);
                //并将该属性名和实例注册到依赖关系映射表dependentBeanMap和dependenciesForBeanMap中
                //dependentBeanMap ： beanA name --> 依赖beanA的 所有bean的名字列表
                //dependenciesForBeanMap ： beanA name --> beanA依赖的 所有bean的名字列表
                registerDependentBean(propertyName, beanName);
                if (logger.isDebugEnabled()) {
                    logger.debug("Added autowiring by name from bean name '" + beanName +
                            "' via property '" + propertyName + "' to bean named '" + propertyName + "'");
                }
            }
            else {
                if (logger.isTraceEnabled()) {
                    logger.trace("Not autowiring property '" + propertyName + "' of bean '" + beanName +
                            "' by name: no matching bean found");
                }
            }
        }
    }

    /**
     * Abstract method defining "autowire by type" (bean properties by type) behavior.
     * <p>This is like PicoContainer default, in which there must be exactly one bean
     * of the property type in the bean factory. This makes bean factories simple to
     * configure for small namespaces, but doesn't work as well as standard Spring
     * behavior for bigger applications.
     * @param beanName the name of the bean to autowire by type
     * @param mbd the merged bean definition to update through autowiring
     * @param bw the BeanWrapper from which we can obtain information about the bean
     * @param pvs the PropertyValues to register wired objects with
     */
    protected void autowireByType(
            String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {

        TypeConverter converter = getCustomTypeConverter();
        if (converter == null) {
            converter = bw;
        }

        Set<String> autowiredBeanNames = new LinkedHashSet<>(4);
        String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
        for (String propertyName : propertyNames) {
            try {
                PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
                // Don't try autowiring by type for type Object: never makes sense,
                // even if it technically is a unsatisfied, non-simple property.
                if (Object.class != pd.getPropertyType()) {
                    MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
                    // Do not allow eager init for type matching in case of a prioritized post-processor.
                    boolean eager = !PriorityOrdered.class.isInstance(bw.getWrappedInstance());
                    DependencyDescriptor desc = new AutowireByTypeDependencyDescriptor(methodParam, eager);
                    Object autowiredArgument = resolveDependency(desc, beanName, autowiredBeanNames, converter);
                    if (autowiredArgument != null) {
                        pvs.add(propertyName, autowiredArgument);
                    }
                    for (String autowiredBeanName : autowiredBeanNames) {
                        registerDependentBean(autowiredBeanName, beanName);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Autowiring by type from bean name '" + beanName + "' via property '" +
                                    propertyName + "' to bean named '" + autowiredBeanName + "'");
                        }
                    }
                    autowiredBeanNames.clear();
                }
            }
            catch (BeansException ex) {
                throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName, propertyName, ex);
            }
        }
    }


    /**
     * Return an array of non-simple bean properties that are unsatisfied.
     * These are probably unsatisfied references to other beans in the
     * factory. Does not include simple properties like primitives or Strings.
     * @param mbd the merged bean definition the bean was created with
     * @param bw the BeanWrapper the bean was created with
     * @return an array of bean property names
     * @see org.springframework.beans.BeanUtils#isSimpleProperty
     */
    protected String[] unsatisfiedNonSimpleProperties(AbstractBeanDefinition mbd, BeanWrapper bw) {
        Set<String> result = new TreeSet<>();
        PropertyValues pvs = mbd.getPropertyValues();
        PropertyDescriptor[] pds = bw.getPropertyDescriptors();
        for (PropertyDescriptor pd : pds) {
            if (pd.getWriteMethod() != null && !isExcludedFromDependencyCheck(pd) && !pvs.contains(pd.getName()) &&
                    !BeanUtils.isSimpleProperty(pd.getPropertyType())) {
                result.add(pd.getName());
            }
        }
        return StringUtils.toStringArray(result);
    }

    /**
     * Extract a filtered set of PropertyDescriptors from the given BeanWrapper,
     * excluding ignored dependency types or properties defined on ignored dependency interfaces.
     * @param bw the BeanWrapper the bean was created with
     * @param cache whether to cache filtered PropertyDescriptors for the given bean Class
     * @return the filtered PropertyDescriptors
     * @see #isExcludedFromDependencyCheck
     * @see #filterPropertyDescriptorsForDependencyCheck(org.springframework.beans.BeanWrapper)
     */
    protected PropertyDescriptor[] filterPropertyDescriptorsForDependencyCheck(BeanWrapper bw, boolean cache) {
        PropertyDescriptor[] filtered = this.filteredPropertyDescriptorsCache.get(bw.getWrappedClass());
        if (filtered == null) {
            filtered = filterPropertyDescriptorsForDependencyCheck(bw);
            if (cache) {
                PropertyDescriptor[] existing =
                        this.filteredPropertyDescriptorsCache.putIfAbsent(bw.getWrappedClass(), filtered);
                if (existing != null) {
                    filtered = existing;
                }
            }
        }
        return filtered;
    }

    /**
     * Extract a filtered set of PropertyDescriptors from the given BeanWrapper,
     * excluding ignored dependency types or properties defined on ignored dependency interfaces.
     * @param bw the BeanWrapper the bean was created with
     * @return the filtered PropertyDescriptors
     * @see #isExcludedFromDependencyCheck
     */
    protected PropertyDescriptor[] filterPropertyDescriptorsForDependencyCheck(BeanWrapper bw) {
        List<PropertyDescriptor> pds = new ArrayList<>(Arrays.asList(bw.getPropertyDescriptors()));
        pds.removeIf(this::isExcludedFromDependencyCheck);
        return pds.toArray(new PropertyDescriptor[0]);
    }

    /**
     * Determine whether the given bean property is excluded from dependency checks.
     * <p>This implementation excludes properties defined by CGLIB and
     * properties whose type matches an ignored dependency type or which
     * are defined by an ignored dependency interface.
     * @param pd the PropertyDescriptor of the bean property
     * @return whether the bean property is excluded
     * @see #ignoreDependencyType(Class)
     * @see #ignoreDependencyInterface(Class)
     */
    protected boolean isExcludedFromDependencyCheck(PropertyDescriptor pd) {
        return (AutowireUtils.isExcludedFromDependencyCheck(pd) ||
                this.ignoredDependencyTypes.contains(pd.getPropertyType()) ||
                AutowireUtils.isSetterDefinedInInterface(pd, this.ignoredDependencyInterfaces));
    }

    /**
     * Perform a dependency check that all properties exposed have been set,
     * if desired. Dependency checks can be objects (collaborating beans),
     * simple (primitives and String), or all (both).
     * @param beanName the name of the bean
     * @param mbd the merged bean definition the bean was created with
     * @param pds the relevant property descriptors for the target bean
     * @param pvs the property values to be applied to the bean
     * @see #isExcludedFromDependencyCheck(java.beans.PropertyDescriptor)
     */
    protected void checkDependencies(
            String beanName, AbstractBeanDefinition mbd, PropertyDescriptor[] pds, PropertyValues pvs)
            throws UnsatisfiedDependencyException {

        int dependencyCheck = mbd.getDependencyCheck();
        for (PropertyDescriptor pd : pds) {
            if (pd.getWriteMethod() != null && !pvs.contains(pd.getName())) {
                boolean isSimple = BeanUtils.isSimpleProperty(pd.getPropertyType());
                boolean unsatisfied = (dependencyCheck == AbstractBeanDefinition.DEPENDENCY_CHECK_ALL) ||
                        (isSimple && dependencyCheck == AbstractBeanDefinition.DEPENDENCY_CHECK_SIMPLE) ||
                        (!isSimple && dependencyCheck == AbstractBeanDefinition.DEPENDENCY_CHECK_OBJECTS);
                if (unsatisfied) {
                    throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName, pd.getName(),
                            "Set this property value or disable dependency checking for this bean.");
                }
            }
        }
    }

    /**
     * Apply the given property values, resolving any runtime references
     * to other beans in this bean factory. Must use deep copy, so we
     * don't permanently modify this property.
     * @param beanName the bean name passed for better exception information
     * @param mbd the merged bean definition
     * @param bw the BeanWrapper wrapping the target object
     * @param pvs the new property values
     */
    //pvs里面保存着这个bean的属性名和属性值(没有经过格式转换的)，bw里面保存PropertyEditor(每次都会重新new PropertyEditor)
    //在这个方法里面使用PropertyEditor转换bean的属性值的格式
    protected void applyPropertyValues(String beanName, BeanDefinition mbd, BeanWrapper bw, PropertyValues pvs) {
        if (pvs.isEmpty()) {
            return;
        }

        if (System.getSecurityManager() != null && bw instanceof BeanWrapperImpl) {
            ((BeanWrapperImpl) bw).setSecurityContext(getAccessControlContext());
        }

        MutablePropertyValues mpvs = null;
        List<PropertyValue> original;

        if (pvs instanceof MutablePropertyValues) {
            mpvs = (MutablePropertyValues) pvs;
            if (mpvs.isConverted()) {
                // Shortcut: use the pre-converted values as-is.
                try {
                    bw.setPropertyValues(mpvs);
                    return;
                }
                catch (BeansException ex) {
                    throw new BeanCreationException(
                            mbd.getResourceDescription(), beanName, "Error setting property values", ex);
                }
            }
            original = mpvs.getPropertyValueList();
        }
        else {
            original = Arrays.asList(pvs.getPropertyValues());
        }

        TypeConverter converter = getCustomTypeConverter();
        if (converter == null) {
            converter = bw;
        }
        BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this, beanName, mbd, converter);

        // Create a deep copy, resolving any references for values.
        List<PropertyValue> deepCopy = new ArrayList<>(original.size());
        boolean resolveNecessary = false;
        for (PropertyValue pv : original) {
            if (pv.isConverted()) {
                deepCopy.add(pv);
            }
            else {
                String propertyName = pv.getName();
                Object originalValue = pv.getValue();
                Object resolvedValue = valueResolver.resolveValueIfNecessary(pv, originalValue);
                Object convertedValue = resolvedValue;
                boolean convertible = bw.isWritableProperty(propertyName) &&
                        !PropertyAccessorUtils.isNestedOrIndexedProperty(propertyName);
                if (convertible) {
                    // PropertyEditor应用 ： ioc容器从这个入口进入
                    // 从BeanWrapperImpl的继承结构和源码可以看出，new 一个BeanWrapperImpl的时候，
                    // 也会new新的PropertyEditorRegistry PropertyEditor赋值给BeanWrapperImpl的相关属性，
                    // 因为PropertyEditor不是线程安全的，里面会存放转换后的结果值，所以Spring会new新的PropertyEditor。

                    // org.springframework.beans.PropertyEditorRegistrySupport.customEditorsForPath属性为例描述一下：
                    // propertyPath ： 比如IndexedTestBean对象中有一个属性List<TestBean>  list， 属性TestBean里面有一个name属性
                    // IndexedTestBean bean = new IndexedTestBean();
                    // BeanWrapper bw = new BeanWrapperImpl(bean);
                    // bw.registerCustomEditor(String.class, "list[0].name", PropertyEditorA);
                    // 这样我们就为bw中的IndexedTestBean bean的属性List<TestBean>[0]的name属性注册了一个属性编辑器， spring会用它来转换PropertyValue中的属性值
                    // bw.getPropertyValue("list[0].name") 通过这个方法，传入这个属性名，就可以获取到PropertyEditorA转换格式后的属性值
                    convertedValue = convertForProperty(resolvedValue, propertyName, bw, converter);
                }
                // Possibly store converted value in merged bean definition,
                // in order to avoid re-conversion for every created bean instance.
                if (resolvedValue == originalValue) {
                    if (convertible) {
                        pv.setConvertedValue(convertedValue);
                    }
                    deepCopy.add(pv);
                }
                else if (convertible && originalValue instanceof TypedStringValue &&
                        !((TypedStringValue) originalValue).isDynamic() &&
                        !(convertedValue instanceof Collection || ObjectUtils.isArray(convertedValue))) {
                    pv.setConvertedValue(convertedValue);
                    deepCopy.add(pv);
                }
                else {
                    resolveNecessary = true;
                    deepCopy.add(new PropertyValue(pv, convertedValue));
                }
            }
        }
        if (mpvs != null && !resolveNecessary) {
            mpvs.setConverted();
        }

        // Set our (possibly massaged) deep copy.
        try {
            bw.setPropertyValues(new MutablePropertyValues(deepCopy));
        }
        catch (BeansException ex) {
            throw new BeanCreationException(
                    mbd.getResourceDescription(), beanName, "Error setting property values", ex);
        }
    }

    /**
     * Convert the given value for the specified target property.
     */
    @Nullable
    private Object convertForProperty(
            @Nullable Object value, String propertyName, BeanWrapper bw, TypeConverter converter) {

        if (converter instanceof BeanWrapperImpl) {
            return ((BeanWrapperImpl) converter).convertForProperty(value, propertyName);
        }
        else {
            PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
            MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
            return converter.convertIfNecessary(value, pd.getPropertyType(), methodParam);
        }
    }


    /**
     * Initialize the given bean instance, applying factory callbacks
     * as well as init methods and bean post processors.
     * <p>Called from {@link #createBean} for traditionally defined beans,
     * and from {@link #initializeBean} for existing bean instances.
     * @param beanName the bean name in the factory (for debugging purposes)
     * @param bean the new bean instance we may need to initialize
     * @param mbd the bean definition that the bean was created with
     * (can also be {@code null}, if given an existing bean instance)
     * @return the initialized bean instance (potentially wrapped)
     * @see BeanNameAware
     * @see BeanClassLoaderAware
     * @see BeanFactoryAware
     * @see #applyBeanPostProcessorsBeforeInitialization
     * @see #invokeInitMethods
     * @see #applyBeanPostProcessorsAfterInitialization
     */
    protected Object initializeBean(final String beanName, final Object bean, @Nullable RootBeanDefinition mbd) {
        //***************************安全校验  begin***********************************
        // System.getSecurityManager()  不用去看，jdk底层的东西
        if (System.getSecurityManager() != null) {
            AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                invokeAwareMethods(beanName, bean);
                return null;
            }, getAccessControlContext());
        }
        else {
            //bean如果实现了BeanNameAware  BeanClassLoaderAware BeanFactoryAware，就在这里给bean注入相关属性
            //spring 内部的一些bean 也通过这个方法作为入口，完成了很多初始化，比如通过new的方式完善自己的属性
            invokeAwareMethods(beanName, bean);
        }
        //***************************安全校验  end*************************************

        Object wrappedBean = bean;
        if (mbd == null || !mbd.isSynthetic()) {
            // for循环执行后置处理BeanPostProcessor的applyBeanPostProcessorsBeforeInitialization方法 ：
            // 1. 执行初始化方法 ：@PostConstruct注解的方法被调用 InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization
            // 2. ...
            wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
        }

        try {
            //执行初始化方法 ： init-mehtod指定的方法 以及实现InitializingBean接口中的afterPropertiesSet方法
            invokeInitMethods(beanName, wrappedBean, mbd);
        }
        catch (Throwable ex) {
            throw new BeanCreationException(
                    (mbd != null ? mbd.getResourceDescription() : null),
                    beanName, "Invocation of init method failed", ex);
        }
        if (mbd == null || !mbd.isSynthetic()) {
            // for循环执行后置处理BeanPostProcessor的applyBeanPostProcessorsAfterInitialization方法 ：
            // 1.开发人员没有自己提供targetSource,动态代理对象就在这里创建 :使用的就是AnnotationAwareAspectJAutoProxyCreator， 最终调用AbstractAutoProxyCreator#postProcessAfterInitialization
            // 2. ...
            wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
        }

        return wrappedBean;
    }

    //bean如果实现了BeanNameAware  BeanClassLoaderAware BeanFactoryAware，就在这里给bean注入相关属性
    private void invokeAwareMethods(final String beanName, final Object bean) {
        if (bean instanceof Aware) {
            if (bean instanceof BeanNameAware) {
                ((BeanNameAware) bean).setBeanName(beanName);
            }
            if (bean instanceof BeanClassLoaderAware) {
                ClassLoader bcl = getBeanClassLoader();
                if (bcl != null) {
                    ((BeanClassLoaderAware) bean).setBeanClassLoader(bcl);
                }
            }
            if (bean instanceof BeanFactoryAware) {
                ((BeanFactoryAware) bean).setBeanFactory(AbstractAutowireCapableBeanFactory.this);
            }
        }
    }

    /**
     * Give a bean a chance to react now all its properties are set,
     * and a chance to know about its owning bean factory (this object).
     * This means checking whether the bean implements InitializingBean or defines
     * a custom init method, and invoking the necessary callback(s) if it does.
     * @param beanName the bean name in the factory (for debugging purposes)
     * @param bean the new bean instance we may need to initialize
     * @param mbd the merged bean definition that the bean was created with
     * (can also be {@code null}, if given an existing bean instance)
     * @throws Throwable if thrown by init methods or by the invocation process
     * @see #invokeCustomInitMethod
     */
    protected void invokeInitMethods(String beanName, final Object bean, @Nullable RootBeanDefinition mbd)
            throws Throwable {

        boolean isInitializingBean = (bean instanceof InitializingBean);
        if (isInitializingBean && (mbd == null || !mbd.isExternallyManagedInitMethod("afterPropertiesSet"))) {
            if (logger.isDebugEnabled()) {
                logger.debug("Invoking afterPropertiesSet() on bean with name '" + beanName + "'");
            }
            if (System.getSecurityManager() != null) {
                try {
                    AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                        ((InitializingBean) bean).afterPropertiesSet();
                        return null;
                    }, getAccessControlContext());
                }
                catch (PrivilegedActionException pae) {
                    throw pae.getException();
                }
            }
            else {
                ((InitializingBean) bean).afterPropertiesSet();
            }
        }

        if (mbd != null && bean.getClass() != NullBean.class) {
            String initMethodName = mbd.getInitMethodName();
            if (StringUtils.hasLength(initMethodName) &&
                    !(isInitializingBean && "afterPropertiesSet".equals(initMethodName)) &&
                    !mbd.isExternallyManagedInitMethod(initMethodName)) {
                invokeCustomInitMethod(beanName, bean, mbd);
            }
        }
    }

    /**
     * Invoke the specified custom init method on the given bean.
     * Called by invokeInitMethods.
     * <p>Can be overridden in subclasses for custom resolution of init
     * methods with arguments.
     * @see #invokeInitMethods
     */
    protected void invokeCustomInitMethod(String beanName, final Object bean, RootBeanDefinition mbd)
            throws Throwable {

        String initMethodName = mbd.getInitMethodName();
        Assert.state(initMethodName != null, "No init method set");
        final Method initMethod = (mbd.isNonPublicAccessAllowed() ?
                BeanUtils.findMethod(bean.getClass(), initMethodName) :
                ClassUtils.getMethodIfAvailable(bean.getClass(), initMethodName));

        if (initMethod == null) {
            if (mbd.isEnforceInitMethod()) {
                throw new BeanDefinitionValidationException("Could not find an init method named '" +
                        initMethodName + "' on bean with name '" + beanName + "'");
            }
            else {
                if (logger.isDebugEnabled()) {
                    logger.debug("No default init method named '" + initMethodName +
                            "' found on bean with name '" + beanName + "'");
                }
                // Ignore non-existent default lifecycle methods.
                return;
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Invoking init method  '" + initMethodName + "' on bean with name '" + beanName + "'");
        }

        if (System.getSecurityManager() != null) {
            AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                ReflectionUtils.makeAccessible(initMethod);
                return null;
            });
            try {
                AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () ->
                        initMethod.invoke(bean), getAccessControlContext());
            }
            catch (PrivilegedActionException pae) {
                InvocationTargetException ex = (InvocationTargetException) pae.getException();
                throw ex.getTargetException();
            }
        }
        else {
            try {
                ReflectionUtils.makeAccessible(initMethod);
                initMethod.invoke(bean);
            }
            catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }
    }


    /**
     * Applies the {@code postProcessAfterInitialization} callback of all
     * registered BeanPostProcessors, giving them a chance to post-process the
     * object obtained from FactoryBeans (for example, to auto-proxy them).
     * @see #applyBeanPostProcessorsAfterInitialization
     */
    @Override
    protected Object postProcessObjectFromFactoryBean(Object object, String beanName) {
        return applyBeanPostProcessorsAfterInitialization(object, beanName);
    }

    /**
     * Overridden to clear FactoryBean instance cache as well.
     */
    @Override
    protected void removeSingleton(String beanName) {
        synchronized (getSingletonMutex()) {
            super.removeSingleton(beanName);
            this.factoryBeanInstanceCache.remove(beanName);
        }
    }

    /**
     * Overridden to clear FactoryBean instance cache as well.
     */
    @Override
    protected void clearSingletonCache() {
        synchronized (getSingletonMutex()) {
            super.clearSingletonCache();
            this.factoryBeanInstanceCache.clear();
        }
    }

    /**
     * Expose the logger to collaborating delegates.
     * @since 5.0.7
     */
    Log getLogger() {
        return logger;
    }


    /**
     * Special DependencyDescriptor variant for Spring's good old autowire="byType" mode.
     * Always optional; never considering the parameter name for choosing a primary candidate.
     */
    @SuppressWarnings("serial")
    private static class AutowireByTypeDependencyDescriptor extends DependencyDescriptor {

        public AutowireByTypeDependencyDescriptor(MethodParameter methodParameter, boolean eager) {
            super(methodParameter, false, eager);
        }

        @Override
        public String getDependencyName() {
            return null;
        }
    }

}

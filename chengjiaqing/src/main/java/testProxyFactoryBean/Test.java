package testProxyFactoryBean;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @date 2020/10/10 17:16
 * @author chengjiaqing
 * @version : 0.1
 */


public class Test {


    public static void main(String[] args) {


        AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext("testProxyFactoryBean");

        //实际的spring使用aop的过程，配置好ProxyFactoryBean，给ProxyFactoryBean设置一个bean id
        //然后通过ac.getBean(bean id),就取得被ProxyFactoryBean代理的对象，不是ProxyFactoryBean
        //因为这个bean id虽然代表ProxyFactoryBean对象，直接getBean获取的是ProxyFactoryBean.getObject()返回的对象，即代理对象
        //ac.getBean(&bean id),才能取得ProxyFactoryBean对象

        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        proxyFactoryBean.setBeanFactory(ac.getBeanFactory());
        //aop拦截处理类
        proxyFactoryBean.setInterceptorNames("myBeforeAop");
        //代理的接口
        proxyFactoryBean.setInterfaces(ServInter.class);
        //被代理对象
        proxyFactoryBean.setTarget(ac.getBean(ServInter.class));
        //放入bean工厂，实际开发是在config下使用注解，设置多个proxyFactoryBean代理，设置不同bean id
        ac.getBeanFactory().registerSingleton("myproxy",proxyFactoryBean);

        ServInter servInterProxy = (ServInter) ac.getBean("myproxy");
        servInterProxy.say();
        //获取直接的ProxyFactoryBean对象，加&
        System.out.println(ac.getBean("&myproxy"));
    }
}
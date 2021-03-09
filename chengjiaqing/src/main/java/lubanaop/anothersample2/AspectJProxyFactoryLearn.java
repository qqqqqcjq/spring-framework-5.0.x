package lubanaop.anothersample2;

import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

/**
 * @date 2021/2/13 12:45
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
//我们这个例子anothersample1使用AspectJProxyFactory.getProxy创建代理对象以及拦截器链Advisors
//Spring 使用的 ProxyFactory 创建代理对象以及拦截器链Advisors

//创建代理对象时，targetsource 和advisors 被封装在 ProxyFactory/AspectJProxyFactory(或其父类中)
//所以，一个target需要new 一个  ProxyFactory/AspectJProxyFactory来创建代理
public class AspectJProxyFactoryLearn {

    public static void main(String[] args) {
        //手工创建一个实例
        TargetService targetService = new TargetServiceImpl();
        //使用AspectJ语法 自动创建代理对象
        AspectJProxyFactory aspectJProxyFactory = new AspectJProxyFactory(targetService);
        //添加切面和通知类
        aspectJProxyFactory.addAspect(AopAdviceConfig.class);
        //创建代理对象
        TargetService proxyService = aspectJProxyFactory.getProxy();
        //进行方法调用
        proxyService.service();
    }
}
package lubanaop.anothersample1;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @date 2021/1/30 13:44
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
@Configuration
@ComponentScan
@EnableAspectJAutoProxy( proxyTargetClass=true)
public class JavaConfig {

    public static void main(String[] args) {
        //1、创建Spring的IOC的容器
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(JavaConfig.class);

        //2、从IOC容器中获取bean的实例
        TargetClass targetClass = (TargetClass) ctx.getBean("targetClass");

        //3、使用bean
        targetClass.joint("spring","aop");
    }
}
package lubanaop.anothersample1;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @date 2021/1/30 13:44
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
@Configuration
@ComponentScan
@EnableAspectJAutoProxy( proxyTargetClass=true)
public class JavaConfig {

    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        //1、创建Spring的IOC的容器
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(JavaConfig.class);

        //2、从IOC容器中获取bean的实例
        TargetClass targetClass = (TargetClass) ctx.getBean("targetClass");

        //3、使用bean
        targetClass.joint("spring","aop");

//        //final修饰的方法
//        targetClass.jointfinal("spring","aop");
//
//        //private修饰的方法
//        Method[] ms = targetClass.getClass().getMethods();
//        Method method = TargetClass.class.getDeclaredMethod("jointprivate",null);
//        //private修饰的方法只能被本类中的其他方法调用，类实例只能调用public或者protected修饰的方法或者变量。
//        //通过反射调用要修改访问权限
//        method.setAccessible(true);
//        method.invoke(targetClass,null);
    }
}
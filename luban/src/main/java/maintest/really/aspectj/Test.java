package maintest.really.aspectj;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @date 2020/7/7 23:52
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class Test {
    public static void main(String[] args) throws Exception {
        ApplicationContext context = new AnnotationConfigApplicationContext(Javaconfig.class);
        DemoBean demoBean = context.getBean(DemoBean.class);
        demoBean.run1();
        demoBean.run2();
    }
}
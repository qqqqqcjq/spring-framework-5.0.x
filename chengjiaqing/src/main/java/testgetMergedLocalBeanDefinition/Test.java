package testgetMergedLocalBeanDefinition;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @date 2020/4/14 22:52
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class Test {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(Myconfig.class);
        System.out.println(((OrderService)annotationConfigApplicationContext.getBean("order")).getName());
        System.out.println(((OrderService)annotationConfigApplicationContext.getBean("orderChild")).getName());
    }
}
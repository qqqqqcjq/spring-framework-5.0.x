package testSpringPlaceholder;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @date 2020/10/14 11:00
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class Test {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(JavaConfig.class);
        System.out.println(annotationConfigApplicationContext.getBean("dataSource").toString());
    }
}
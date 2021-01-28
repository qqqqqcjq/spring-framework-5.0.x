package testSpringEL;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @date 2020/10/12 23:39
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class Test {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(JavaConfig.class);
        System.out.println(((LoginController) annotationConfigApplicationContext.getBean("loginController")).getJdbcUrl());
    }
}
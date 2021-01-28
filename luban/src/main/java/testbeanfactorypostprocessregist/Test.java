package testbeanfactorypostprocessregist;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @date 2020/9/21 11:07
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class Test {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(JavaConfig.class);

    }
}
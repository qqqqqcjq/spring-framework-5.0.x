package com.jiaqing.ioc.test.lazy;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @date 2020/7/5 15:32
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class Test {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(JavaConfig.class);
        annotationConfigApplicationContext.getBean("lazyBean");
    }
}
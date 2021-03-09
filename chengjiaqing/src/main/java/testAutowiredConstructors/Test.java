package testAutowiredConstructors;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @date 2020/8/18 17:58
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class Test {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(JavaConfig.class);
        IndexService indexService = (IndexService) annotationConfigApplicationContext.getBean("indexService");
    }
}
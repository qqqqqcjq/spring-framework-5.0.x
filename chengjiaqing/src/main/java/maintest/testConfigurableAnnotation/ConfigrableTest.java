package maintest.testConfigurableAnnotation;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @date 2020/9/24 14:08
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class ConfigrableTest {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(Config.class);
        maintest.testConfigurableAnnotation.Account account = new maintest.testConfigurableAnnotation.Account();
        account.output();
    }
}
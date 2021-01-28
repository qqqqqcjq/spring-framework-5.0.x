package testSomeSimple;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @date 2020/10/9 13:48
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
@Configuration
@ComponentScan("testSomeSimple")
public class TestInjectMethodParam {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(TestInjectMethodParam.class);
        People people = (People) annotationConfigApplicationContext.getBean("people");
        people.walk();
    }
}


package maintest.dependson;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @date 2020/7/9 14:41
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class Test {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(Config.class);

    }
}
package testCircleRefrence;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @date 2021/2/1 11:01
 * @author chengjiaqing
 * @version : 0.1
 */ 
@EnableAspectJAutoProxy
@Configuration
@ComponentScan("testCircleRefrence")
public class JavaConfig {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(JavaConfig.class);
    }
}
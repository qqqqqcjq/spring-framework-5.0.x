package testgetMergedLocalBeanDefinition;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * @date 2020/4/14 22:53
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
@Configuration
@ComponentScan("testgetMergedLocalBeanDefinition")
@ImportResource("classpath:testgetMergedLocalBeanDefinition.xml")
public class Myconfig {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(Myconfig.class);

    }
}
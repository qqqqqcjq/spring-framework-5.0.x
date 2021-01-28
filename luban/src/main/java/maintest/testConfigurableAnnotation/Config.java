package maintest.testConfigurableAnnotation;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableLoadTimeWeaving;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;

/**
 * @date 2020/9/24 14:08
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
@Configuration
@ComponentScan
@EnableLoadTimeWeaving
@EnableSpringConfigured
public class Config {
}
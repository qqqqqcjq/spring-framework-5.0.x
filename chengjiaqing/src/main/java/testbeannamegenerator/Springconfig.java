package testbeannamegenerator;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @date 2019/12/7 17:44
 * @author chengjiaqing
 * @version : 0.1
 */ 
 @Configuration
 @ComponentScan(basePackages = "testbeannamegenerator",nameGenerator = MyBeanNameGenerator.class)
  public class Springconfig {
}
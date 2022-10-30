package com.jiaqing.ioc;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * @date 2019/12/20 12:26
 * @author chengjiaqing
 * @version : 0.1
 */ 
 @Configuration
 @ComponentScan("com")
 @ImportResource("classpath:some_service.xml")
 public class JavaConfig {
}
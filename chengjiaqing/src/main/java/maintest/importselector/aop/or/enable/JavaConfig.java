package maintest.importselector.aop.or.enable;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @date 2020/1/2 15:27
 * @author chengjiaqing
 * @version : 0.1
 */ 
 @Configuration
 @ComponentScan("maintest.importselector.aop.or.enable")
 @EnableMyAop
 @Import(ImportOrdinaryBean.class)
 public class JavaConfig {


}
package testfactorybean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import testATEbean.DataSourceManager;

/**
 * @date 2019/12/7 17:44
 * @author chengjiaqing
 * @version : 0.1
 */ 
 @Configuration
 @ComponentScan(basePackages = "testfactorybean")
  public class Springconfig {

}
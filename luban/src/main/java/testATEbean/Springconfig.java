package testATEbean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * @date 2019/12/7 17:44
 * @author chengjiaqing
 * @version : 0.1
 */ 
 @Configuration
 @ComponentScan(basePackages = "testATEbean")
  public class Springconfig {

 	@Bean("dataSourceManager")
	public static DataSourceManager firstdatasour(){
		System.out.println("createdatasourcemanager1");
 		return new DataSourceManager("cjq","123","localhaot","7001");
	}
}
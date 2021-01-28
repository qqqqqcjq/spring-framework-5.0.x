package testPropertyEditor;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * @date 2019/12/20 12:26
 * @author chengjiaqing
 * @version : 0.1
 */ 
 @Configuration
 @ComponentScan("testPropertyEditor")
 @ImportResource("classpath:testPropertyEditor.xml")
  public class JavaConfig {
}
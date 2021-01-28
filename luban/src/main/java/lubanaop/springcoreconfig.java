package lubanaop;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @date 2019/12/10 15:06
 * @author chengjiaqing
 * @version : 0.1
 */ 
 @Configuration
 @ComponentScan("lubanaop")
 @EnableAspectJAutoProxy // @EnableAspectJAutoProxy( proxyTargetClass=false) /
  public class springcoreconfig {
}
package maintest.coniguration.enhancer;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @date 2020/1/3 17:20
 * @author chengjiaqing
 * @version : 0.1
 */


 @Configuration
 //如果不加@configruation，那么就是使用原生的配置类
 //如果加了@configruation，那么在enhanceConfigurationClasses(beanFactory)使用cglib给配置类生成一个代理类，
 //这个代理类是配置类的子类，并且会设置继承EnhancedConfiguration extends BeanFactoryAware接口(如下代码)，这样代理类就可以使用BeanFactory,
 //enhancer.setInterfaces(new Class<?>[] {EnhancedConfiguration.class});
 //public interface EnhancedConfiguration extends BeanFactoryAware {}
 //然后在@Bean中每次new bean之前，先使用beanFactory获取一下，获取到了不创建，没有获取到再创建，这样就不会违反spring的singleton bean规则

  public class JavaConfig {

 	@Bean
	public  IndexDaoImpl1 indexDaoImpl1(){
		System.out.println("init IndexDaoImpl1");
 		return new IndexDaoImpl1();
	}

	@Bean
	public IndexDaoImpl2 indexDaoImpl2(){
		indexDaoImpl1();
		return new IndexDaoImpl2();
	}
}
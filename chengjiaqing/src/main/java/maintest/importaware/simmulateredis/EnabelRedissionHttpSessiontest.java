package maintest.importaware.simmulateredis;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @date 2020/1/22 14:45
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
@Retention(RetentionPolicy.RUNTIME)
//因为一个类加了@Configuration注解后，他就不会被当作一个普通的bean放进singleObjects中，而是会使用cglib进行增强后的实例放入singleObjects
//另外，构造容器时只能传一个@Configuration配置类, @Configuration又不是@Component体系的，所以为了让RedissionHttpSessionConfiguirationtest放进ioc容器中，我们这里还要用import导入
//当然@Import({RedissionHttpSessionConfiguirationtest.class})放在其他地方也可以，但是和EnabelRedissionHttpSessiontest注解放在一起的话，我们就可以用在javaconfig类上加上@EnabelRedissionHttpSessiontest(keeptime = 900,key = "hahah")一句代码来动态的开启这个功能。
//上面这种开启功能的方式很多Enable都是这样的原理
@Import({RedissionHttpSessionConfiguirationtest.class})
public @interface EnabelRedissionHttpSessiontest {

	int keeptime() default 18000;
	String key() default "";

}
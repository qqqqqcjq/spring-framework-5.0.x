package maintest.importaware.simmulateredis;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @date 2020/1/22 15:07
 * @author chengjiaqing
 * @version : 0.1
 */ 

public class Test {


	public static void main(String[] args) {
		AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(JavaConifg.class);
		System.out.println(annotationConfigApplicationContext.getBean(RedissionHttpSessionConfiguirationtest.class).getKey());

	}
}
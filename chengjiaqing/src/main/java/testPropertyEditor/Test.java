package testPropertyEditor;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @date 2019/12/20 12:26
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  public class Test {

	public static void main(String[] args) {

        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(JavaConfig.class);
        System.out.println(annotationConfigApplicationContext.getBean("userManager"));
    }


}
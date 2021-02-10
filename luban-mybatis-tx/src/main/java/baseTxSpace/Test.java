package baseTxSpace;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @date 2021/2/9 13:41
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class Test {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(JavaConfig.class);
        UserService userService = (UserService)annotationConfigApplicationContext.getBean("userService");
        userService.addUser();
    }
}
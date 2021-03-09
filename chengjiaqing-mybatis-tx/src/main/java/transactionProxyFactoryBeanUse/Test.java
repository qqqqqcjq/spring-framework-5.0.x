package transactionProxyFactoryBeanUse;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @date 2021/2/9 13:41
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class Test {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(JavaConfig.class);

        //==========!!!!!!!!!!!!!!!!!!!!!!!! begin !!!!!!!!!!!!!!!!!!!!!!!=================================
        /**
         * 这种方式我们直接使用TransactionInterceptor ProxyFactoryBean生成代理类，生成的规则我们可以设定，可以细化
         */
        //==========!!!!!!!!!!!!!!!!!!!!!!!! end   !!!!!!!!!!!!!!!!!!!!!!!=================================
        UserService userService = (UserService)annotationConfigApplicationContext.getBean("userServiceProxy");
        userService.addUser();
    }
}
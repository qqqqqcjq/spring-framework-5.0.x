package maintest.genericTypeAwareAutowireCandidateResolver;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @date 2020/4/8 16:42
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class Test {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(MyConfiguration.class);
        BaseService  baseService = (BaseService) annotationConfigApplicationContext.getBean("userService");
        System.out.println(baseService.getRepository().toString());

        baseService = (BaseService) annotationConfigApplicationContext.getBean("organizationService");
        System.out.println(baseService.getRepository().toString());

    }
}
package testMapforManyImpl;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @date 2020/4/16 16:03
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class Test {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(JavaConfig.class);
        IndexService indexService = (IndexService)annotationConfigApplicationContext.getBean("indexService");
        indexService.setIndexDaoName("indexDaoImplA");
        System.out.println(indexService.getIndexDao());

        indexService.setIndexDaoName("indexDaoImplB");
        System.out.println(indexService.getIndexDao());
    }
}
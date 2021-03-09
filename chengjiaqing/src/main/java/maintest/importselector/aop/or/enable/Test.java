package maintest.importselector.aop.or.enable;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @date 2020/1/2 16:03
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  public class Test {

  public static void main(String[] args) {
    AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(JavaConfig.class);
    IndexDao indexdao = (IndexDao) annotationConfigApplicationContext.getBean("indexDaoImpl");
    indexdao.query();
  }
}
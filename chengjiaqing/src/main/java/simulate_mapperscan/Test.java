package simulate_mapperscan;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @date 2019/12/29 23:57
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
public class Test {

  public static void main(String[] args) {
    AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
    annotationConfigApplicationContext.register(JConfig.class);
    annotationConfigApplicationContext.refresh();

    CardDao cardDao = (CardDao) annotationConfigApplicationContext.getBean("cardDao");
    cardDao.query("id");
  }
}
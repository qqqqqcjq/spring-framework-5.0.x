package lubanaop;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @date 2019/12/10 15:22
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  public class Aoptest {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(springcoreconfig.class);
		Dao dao = (Dao) annotationConfigApplicationContext.getBean("indexDao");
		dao.query("hhh");

		Dao dao1 = (Dao) annotationConfigApplicationContext.getBean("indexDao");
		dao1.query("hhh");
//		System.out.println(dao instanceof  IndexDao);
//
//		Dao dao1 = (Dao) annotationConfigApplicationContext.getBean("orderDao");
//		dao1.query("introductions");

		System.out.println(dao.getClass().getResource("/").getPath());
	}
}
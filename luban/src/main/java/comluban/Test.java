package comluban;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import testfactorybean.FactoryBeanDao;
import testfactorybean.FactoryBeanbean;


/**
 * @date 2019/12/7 15:00
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  public class Test {



	public static void main(String[] args) {

		AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(Springconfig.class);
		IndexService service = (IndexService) annotationConfigApplicationContext.getBean("indexService");
		service.service();
        testAnnotationForSql(annotationConfigApplicationContext);

	}

	public static void testAnnotationForSql(AnnotationConfigApplicationContext annotationConfigApplicationContext){
		IndexDaoImpl indexDao = (IndexDaoImpl) annotationConfigApplicationContext.getBean("indexDaoImpl");
		System.out.println(IndexDaoImpl.class.isAnnotationPresent(Entity.class));
		Entity entity = IndexDaoImpl.class.getAnnotation(Entity.class);
		System.out.println(entity.toString());
		System.out.println(entity.value());
	}



}
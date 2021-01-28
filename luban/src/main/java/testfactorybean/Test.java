package testfactorybean;

import comluban.Entity;
import comluban.IndexDaoImpl;
import comluban.IndexService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


/**
 * @date 2019/12/7 15:00
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  public class Test {



	public static void main(String[] args) {

		AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(Springconfig.class);
        FactoryBeanDao factoryBeanDao = (FactoryBeanDao) annotationConfigApplicationContext.getBean("factoryBeanbean");
        factoryBeanDao.query();

        FactoryBeanbean factoryBeanbean = (FactoryBeanbean) annotationConfigApplicationContext.getBean("&factoryBeanbean");
        factoryBeanbean.query();

	}



}
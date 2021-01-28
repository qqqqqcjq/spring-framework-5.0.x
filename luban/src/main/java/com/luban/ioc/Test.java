package com.luban.ioc;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @date 2019/12/20 12:26
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  public class Test {

	public static void main(String[] args) {
		//testjavaConfigClass();
		testRegisterOneBean();
        //testApplicationListener();
        //testLzay();
	}

    public static void testLzay(){
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(JavaConfig.class);
        annotationConfigApplicationContext.getBean("lazyDao");
    }

	public static void testApplicationListener(){
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(JavaConfig.class);
        annotationConfigApplicationContext.start();
    }

	public static  void testjavaConfigClass(){
		AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
		annotationConfigApplicationContext.register(JavaConfig.class);
		annotationConfigApplicationContext.addBeanFactoryPostProcessor(new TestBeanFactoryPostProcessor());
		annotationConfigApplicationContext.refresh();
		Service service = (Service) annotationConfigApplicationContext.getBean("service");
		service.query();
	}

	public static void testRegisterOneBean(){
		AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
		annotationConfigApplicationContext.register(IndexDao.class);
        //annotationConfigApplicationContext.registerBean("aa",IndexDao.class);
		annotationConfigApplicationContext.refresh();
		IndexDao indexDao = (IndexDao) annotationConfigApplicationContext.getBean("indexDao");
		indexDao.query();
	}
}
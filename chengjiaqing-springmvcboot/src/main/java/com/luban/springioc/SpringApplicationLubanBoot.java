package com.luban.springioc;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletException;
import java.io.File;

/**
 * @date 2020/3/2 11:39
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class SpringApplicationLubanBoot {
	public static  void run() throws ServletException {

		AnnotationConfigWebApplicationContext annotationConfigWebApplicationContext = new AnnotationConfigWebApplicationContext();
		annotationConfigWebApplicationContext.register(JavaConfig.class);
		annotationConfigWebApplicationContext.refresh();
		DispatcherServlet dispatcherServlet = new DispatcherServlet(annotationConfigWebApplicationContext);


		Tomcat tomcat = new Tomcat();
		tomcat.setPort(8080);

		//告訴tomcat你的源碼在哪裏
		Context ctx = tomcat.addContext("/luban-springmvcboot",new File("luban-springmvcboot/src/main/webapp").getAbsolutePath());
		Tomcat.addServlet(ctx,"XX",dispatcherServlet);
		ctx.addServletMapping("/","xx");

		try {
			tomcat.start();
			tomcat.getServer().await();
		} catch (LifecycleException e) {
			e.printStackTrace();
		}

	}
}
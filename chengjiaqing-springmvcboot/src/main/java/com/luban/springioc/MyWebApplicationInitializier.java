package com.luban.springioc;

import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

/**
 * @date 2020/2/28 10:52
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class MyWebApplicationInitializier implements WebApplicationInitializer {

    public MyWebApplicationInitializier() {
        System.out.println("MyWebApplicationInitializier");
    }

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        AnnotationConfigWebApplicationContext annotationConfigWebApplicationContext = new AnnotationConfigWebApplicationContext();
        annotationConfigWebApplicationContext.register(JavaConfig.class);
        annotationConfigWebApplicationContext.refresh();
        DispatcherServlet dispatcherServlet = new DispatcherServlet(annotationConfigWebApplicationContext);
        ServletRegistration.Dynamic registration = servletContext.addServlet("xx",dispatcherServlet);
        registration.addMapping("/");
        registration.setLoadOnStartup(1);
    }
}
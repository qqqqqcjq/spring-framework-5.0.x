package testbeanfactorypostprocessor;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @date 2020/11/25 17:52
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class Test {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(JavaConfig.class);
        TestBeanFactoryPostProcessorBean testBeanFactoryPostProcessorBean1
                = (TestBeanFactoryPostProcessorBean) annotationConfigApplicationContext.getBean("testBeanFactoryPostProcessorBean");

        TestBeanFactoryPostProcessorBean testBeanFactoryPostProcessorBean2
                = (TestBeanFactoryPostProcessorBean) annotationConfigApplicationContext.getBean("testBeanFactoryPostProcessorBean");

        System.out.println(testBeanFactoryPostProcessorBean1.hashCode()+ "   " + testBeanFactoryPostProcessorBean2.hashCode());

    }
}
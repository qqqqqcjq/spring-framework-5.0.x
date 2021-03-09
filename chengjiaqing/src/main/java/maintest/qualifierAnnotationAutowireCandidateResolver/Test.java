package maintest.qualifierAnnotationAutowireCandidateResolver;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @date 2020/4/9 9:50
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class Test {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(JavaConfig.class);
        EmployeeInfoControl employeeInfoControl = (EmployeeInfoControl)annotationConfigApplicationContext.getBean("employeeInfoControl");
        System.out.println(employeeInfoControl.getEmployeeService1().toString());
        System.out.println(employeeInfoControl.getEmployeeService().toString());

    }
}
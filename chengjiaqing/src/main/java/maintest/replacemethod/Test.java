package maintest.replacemethod;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @date 2020/4/10 17:52
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class Test {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext("classpath:testreplacemethod.xml");
        MyValueCalculator myValueCalculator = (MyValueCalculator)classPathXmlApplicationContext.getBean("myValueCalculator");
        System.out.println(myValueCalculator.computeValue("myValueCalculator param"));
    }
}
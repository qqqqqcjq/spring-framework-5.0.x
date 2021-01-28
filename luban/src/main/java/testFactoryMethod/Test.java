package testFactoryMethod;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @date 2020/11/27 16:03
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class Test {

    public static void main(String[] args) {
        teststaticfactorymethod();
        testinstancefactorymethod();
    }
    public static void teststaticfactorymethod(){
        ApplicationContext ctx = new ClassPathXmlApplicationContext("teststaticfactorymethod.xml");
        Car car1 = (Car) ctx.getBean("bmwCar");
        System.out.println(car1);

        car1 = (Car) ctx.getBean("audiCar");
        System.out.println(car1);
    }

    public static void testinstancefactorymethod(){
        ApplicationContext ctx = new ClassPathXmlApplicationContext("testinstancefactorymethod.xml");
        Car car1 = (Car) ctx.getBean("car4");
        System.out.println(car1);

        car1 = (Car) ctx.getBean("car6");
        System.out.println(car1);
    }
}
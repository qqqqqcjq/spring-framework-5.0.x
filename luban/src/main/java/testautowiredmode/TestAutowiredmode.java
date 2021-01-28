package testautowiredmode;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @date 2020/11/9 19:28
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class TestAutowiredmode {

    public static void main(String[] args) {
        ApplicationContext ac = new ClassPathXmlApplicationContext("classpath:testautowiredmode.xml");
        TeacherServiceImpl teacherServiceImpl = (TeacherServiceImpl) ac.getBean("teacherServiceImpl");
        teacherServiceImpl.say();
    }

}
package maintest.lookupmethod;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @date 2020/4/10 10:15
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class Test
{
    public static void main(String[] args) {
        ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext("classpath:testlookupmethod.xml");
        CommandManager commandManager = (CommandManager)classPathXmlApplicationContext.getBean("commandManager");
        commandManager.process(null);
    }
}
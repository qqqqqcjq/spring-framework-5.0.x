package maintest.testConfigurableAnnotation;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * @date 2020/9/24 14:07
 * @author chengjiaqing
 * @version : 0.1
 */


// Account类，使用new操作符号手动创建，不交由Spring Container管理
@Configurable(autowire = Autowire.BY_TYPE)
public class Account {

    @Autowired
    public Dog dog;

    public void output(){
        System.out.println(dog.getName());
    }

}
package testbeannameaware;

import org.springframework.beans.factory.BeanNameAware;

/**
 * @date 2020/9/21 14:25
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class MyBean implements BeanNameAware {

    String nametouser;
    @Override
    //spring容器会把给bean的名字赋值给nametouser, 用来做什么就是我们自己决定了
    public void setBeanName(String name) {
        this.nametouser = name;
    }
}
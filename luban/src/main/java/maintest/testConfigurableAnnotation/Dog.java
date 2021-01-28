package maintest.testConfigurableAnnotation;

import org.springframework.stereotype.Component;

/**
 * @date 2020/9/24 14:07
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
@Component
public class Dog {
    private String name;
    public Dog() {
        name = "keji";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
package testProxyFactoryBean;

import org.springframework.stereotype.Service;

/**
 * @date 2020/10/10 17:14
 * @author chengjiaqing
 * @version : 0.1
 */


@Service
public class MyService implements ServInter {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void say() {

        System.out.println("MyService say:" + name);
    }
}
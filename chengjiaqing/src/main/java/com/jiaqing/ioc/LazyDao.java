package com.jiaqing.ioc;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @date 2020/4/15 22:59
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
@Lazy
@Component
public class LazyDao {

    public LazyDao() {
        System.out.println("init lazydao");
    }

    private String info;

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
package com.jiaqing.springmvc;

import org.springframework.stereotype.Component;

/**
 * @date 2020/4/12 16:43
 * @author chengjiaqing
 * @version : 0.1
 */




@Component
public class UserPreferences {
    private Long timeMilis;

    public UserPreferences() {
        this.timeMilis = System.currentTimeMillis();
    }

    public void printTime() {
        System.out.println("timeMils:" + timeMilis);
    }
}
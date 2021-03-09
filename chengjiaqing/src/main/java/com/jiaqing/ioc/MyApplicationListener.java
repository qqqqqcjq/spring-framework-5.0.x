package com.jiaqing.ioc;
/** 
 * @date 2020/4/12 14:13
 * @author chengjiaqing
 * @version : 0.1
 */


import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class MyApplicationListener implements ApplicationListener {
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        System.out.println("MyApplicationListener 类接收事件 " + event);
    }
}
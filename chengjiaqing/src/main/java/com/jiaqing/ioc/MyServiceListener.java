package com.jiaqing.ioc;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @date 2020/3/13 21:52
 * @author chengjiaqing
 * @version : 0.1
 */


@Component
public class MyServiceListener {

	@EventListener(classes = ApplicationEvent.class)
	public void myService(ApplicationEvent event){
		System.out.println("MyServiceListener类接收事件："+ event);
	}
}


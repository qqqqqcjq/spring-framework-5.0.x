package com.luban.springmvc.testScope;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

/**
 * @date 2021/1/26 20:49
 * @author chengjiaqing
 * @version : 0.1
 */


@Component
//@Scope(value=WebApplicationContext.SCOPE_SESSION)
@Scope(value= WebApplicationContext.SCOPE_SESSION,proxyMode= ScopedProxyMode.INTERFACES)
public class SessionBean implements ISessionBean {
    private UUID uuid;
    public SessionBean(){
        uuid = UUID.randomUUID();
    }
    public void printId(){
        System.out.println("SessionBean:"+uuid);
    }
}
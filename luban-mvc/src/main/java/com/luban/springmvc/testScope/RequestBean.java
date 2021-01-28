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
@Scope(value = WebApplicationContext.SCOPE_REQUEST,proxyMode= ScopedProxyMode.INTERFACES)
public class RequestBean implements IRequestBean {
    private UUID uuid;

    public RequestBean() {
        uuid = UUID.randomUUID();
    }

    public void printId() {
        System.out.println("RequestBean:" + uuid);
    }
}
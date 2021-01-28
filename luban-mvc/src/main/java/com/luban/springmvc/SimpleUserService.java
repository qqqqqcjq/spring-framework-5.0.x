package com.luban.springmvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

/**
 * @date 2020/4/12 16:43
 * @author chengjiaqing
 * @version : 0.1
 */

@Service
public class SimpleUserService implements RequestScopeProxyMode {

    @Autowired
    private UserPreferences userPreferences;

    public UserPreferences getUserPreferences() {
        return userPreferences;
    }

    public void setUserPreferences(UserPreferences userPreferences) {
        this.userPreferences = userPreferences;
    }

    public void setPrototypeBean(UserPreferences userPreferences) {
        this.userPreferences = userPreferences;
    }

    public void printTime() {
        userPreferences.printTime();
    }

}
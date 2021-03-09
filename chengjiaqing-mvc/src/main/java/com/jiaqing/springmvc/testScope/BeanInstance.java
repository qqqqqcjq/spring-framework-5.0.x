package com.jiaqing.springmvc.testScope;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @date 2021/1/26 20:49
 * @author chengjiaqing
 * @version : 0.1
 */


@Service
//@Scope(value= ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BeanInstance {
    @Autowired
    private IRequestBean requestBean;
    @Autowired
    private ISessionBean sessionBean;
    public IRequestBean getRequestBean() {
        return requestBean;
    }
    public ISessionBean getSessionBean() {
        return sessionBean;
    }
}
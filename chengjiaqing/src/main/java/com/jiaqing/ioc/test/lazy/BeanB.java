package com.jiaqing.ioc.test.lazy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @date 2020/7/5 16:05
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
@Component
public class BeanB {

    @Autowired
    private BeanA beanA;
}
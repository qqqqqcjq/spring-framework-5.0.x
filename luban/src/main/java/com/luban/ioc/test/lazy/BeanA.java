package com.luban.ioc.test.lazy;

import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * @date 2020/7/5 16:05
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
@Component
@DependsOn("beanB")
public class BeanA {
}
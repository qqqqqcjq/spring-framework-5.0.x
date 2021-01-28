package com.luban.springmvc;

import org.springframework.format.datetime.DateFormatter;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * @date 2020/10/22 14:23
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
@ControllerAdvice
public class GlobalHandler {
    //在@ControllerAdvice注解的类里添加@InitBinder， 在@ControllerAdvice注解的类里添加@InitBinder。这种方式对所有的controller有效
//    @InitBinder
//    public void initDateFormate(WebDataBinder dataBinder) {
//        dataBinder.addCustomFormatter(new DateFormatter("yyyy-MM-dd HH:mm:ss"));
//    }
}
package com.luban.springioc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @date 2020/2/23 13:16
 * @author chengjiaqing
 * @version : 0.1
 */


//@Controller
//@RequestMapping("/mvc")
//public class mvcController {
//
//    @RequestMapping("/hello.do")
//    public String hello(){
//        System.out.println("hello");
//        return "hello";
//    }
//}

@Controller
@RequestMapping("/mvc")
public class mvcController  {

    @RequestMapping("/hello.do")
    public String hello(){
        System.out.println("hello");
        return "hello";
    }


}
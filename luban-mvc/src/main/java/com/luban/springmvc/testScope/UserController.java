package com.luban.springmvc.testScope;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @date 2021/1/26 20:50
 * @author chengjiaqing
 * @version : 0.1
 */


@Controller
@RequestMapping("/user")
// 添加session信息的注解，可以实现 session信息与map的映射 赋值
@SessionAttributes("user")
public class UserController {
    @Autowired
    private BeanInstance beanInstance1;
    @Autowired
    private BeanInstance beanInstance2;

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login(String name, Model model, HttpServletRequest request, HttpSession session) {
        model.addAttribute("user", name);
        System.out.println("SessionBean-1");
        beanInstance1.getSessionBean().printId();
        System.out.println("SessionBean-2");
        beanInstance2.getSessionBean().printId();
        System.out.println("RequestBean-1");
        beanInstance1.getRequestBean().printId();
        System.out.println("RequestBean-2");
        beanInstance2.getRequestBean().printId();
        return "user/check";
    }
    /**
     * 检查自动装载的信息
     * @param model
     * @param request
     * @param session
     * @return
     */
    @RequestMapping(value = "/check", method = RequestMethod.GET)
    public String check(Model model, HttpServletRequest request, HttpSession session) {
        System.out.println("SessionBean-1");
        beanInstance1.getSessionBean().printId();
        System.out.println("SessionBean-2");
        beanInstance2.getSessionBean().printId();
        System.out.println("RequestBean-1");
        beanInstance1.getRequestBean().printId();
        System.out.println("RequestBean-2");
        beanInstance2.getRequestBean().printId();
        return "user/check";
    }
}
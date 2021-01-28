package com.luban.springioc;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;

/**
 * @date 2020/6/3 23:36
 * @author chengjiaqing
 * @version : 0.1
 */


@Controller
@RequestMapping("/sessionattr/demo")
@SessionAttributes(value = {"book", "description"}, types = {Double.class})
public class RedirectController {

    @RequestMapping("/index")
    public String index(Model model, HttpSession httpSession) {
        model.addAttribute("book", "天龙八部");
        model.addAttribute("description", "我乔峰是个契丹人");
        model.addAttribute("price", new Double("1000.00"));

        // 通过Sesson API手动放一个进去
        httpSession.setAttribute("hero", "fsx");

        //跳转之前将数据保存到Model中，因为注解@SessionAttributes中有，所以book和description应该都会保存到SessionAttributes里（注意：不是session里）
        return "redirect:get";
    }

    // 关于@ModelAttribute 下文会讲
    @RequestMapping("/get")
    public String get(@ModelAttribute("book") String book, ModelMap model, HttpSession httpSession, SessionStatus sessionStatus, HttpServletRequest request) throws UnsupportedEncodingException {
        //可以从model中获得book、description和price的参数
        System.out.println(model.get("book") + ";" + model.get("description") + ";" + model.get("price"));


        // 从sesson中也能拿到值
        System.out.println(httpSession.getAttribute("book"));
        System.out.println("API方式手动放进去的：" + httpSession.getAttribute("hero"));
        // 使用@ModelAttribute也能拿到值
        System.out.println(book);

        // 手动清除SessionAttributes
        sessionStatus.setComplete();
        return "redirect:complete";
    }

    @RequestMapping("/complete")
    @ResponseBody
    public String complete(ModelMap modelMap, HttpSession httpSession) {
        //已经被清除，无法获取book的值
        System.out.println(modelMap.get("book"));
        System.out.println("API方式手动放进去的：" + httpSession.getAttribute("hero"));
        return "sessionAttributes";
    }

}
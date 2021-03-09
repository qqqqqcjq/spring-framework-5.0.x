package com.jiaqing.springmvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * @date 2020/2/23 13:16
 * @author chengjiaqing
 * @version : 0.1
 */


@Controller
@RequestMapping("/greet")
@PropertySource("classpath:my.properties") //一般习惯写在@Configuration注解的类文件中
public class mvcController {

    @Autowired
    SimpleUserService simpleUserService;

    @RequestMapping("/testRequestScope")
    public String testRequestScope(HttpServletRequest request) throws IOException {

        return "testRequestScope";
    }


    @RequestMapping("/hello")
    public String hello(String name, String password, @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")Date date, HttpServletResponse response, HttpServletRequest request) throws IOException {
        System.out.println(name + " " + password);
        response.addCookie(new Cookie("TOKEN","12345678910"));
        request.getInputStream();
        return "/hello";
    }

    @RequestMapping("/helloreturn")
    //@ResponseBody  需要引入相关的jar包，否则会提示没有找到convert
    public Map<String,String> helloReturn() throws IOException {
        Map<String,String> temp = new HashMap<String,String> ();
        temp.put("house","malu");
        temp.put("car","legs");
        return temp;
    }

    @RequestMapping("/test")
    public int test(@Value("#{T(Integer).parseInt('${test.myage:10}') + 10}") int myAge) {
      System.out.println(myAge);
      return myAge;
    }

    @RequestMapping("/book")
    public String book(Book book){
        return null;
    }

    @RequestMapping("/date")
    public String date(Date date){
        System.out.println(date.toString());
        return null;
    }

    //在controller里添加@InitBinder， 在@ControllerAdvice注解的类里添加@InitBinder。这种方式只对这个controller有效
//    @InitBinder
//    public void initDateFormate(WebDataBinder dataBinder) {
//        dataBinder.addCustomFormatter(new DateFormatter("yyyy-MM-dd HH:mm:ss"));
//    }

    public mvcController() {
        System.out.println("create 2 of mvcController");
    }
}
package SpringAnnotationUtilTest;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RequestMapping;


import java.lang.annotation.*;

/**
 * @date 2020/10/4 10:39
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class AnnotationUtilTest {
    // 现在我们来获取类上面的注解如下
    public static void main1(String[] args) {
        MyAnno anno1 = Eat.class.getAnnotation(MyAnno.class);
        MyAnno anno2 = Parent.class.getAnnotation(MyAnno.class);
        MyAnno anno3 = Child.class.getAnnotation(MyAnno.class);
        System.out.println(anno1); //@SpringUtilTest.MyAnno()
        System.out.println(anno2); //null
        System.out.println(anno3); //null
    }

    public static void main(String[] args) {
        MyAnno anno1 = Eat.class.getAnnotation(MyAnno.class);

        // 注解交给这么一处理  相当于就会被Spring代理了  这就是优势
        MyAnno sAnno1 = AnnotationUtils.getAnnotation(anno1, MyAnno.class);
        System.out.println(sAnno1); //@SpringUtilTest.MyAnno()
        System.out.println(sAnno1.getClass()); //class SpringUtilTest.$Proxy2
        System.out.println(sAnno1.getClass().getSuperclass());
        System.out.println(sAnno1.getClass().getInterfaces());

        // 这样前后类型不一致的话，会把这个注解上面的注解给获取出来
        RequestMapping annotation = AnnotationUtils.getAnnotation(anno1, RequestMapping.class);
        System.out.println(annotation); //@org.springframework.web.bind.annotation.RequestMapping
    }
}

@MyAnno
interface Eat {
}

class Parent implements Eat {
}

class Child extends Parent {
}

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@RequestMapping // 特意注解上放一个注解，方面测试看结果
//注解标注了@Inherited表示该注解可以被继承，但是anno2和anno3还是null。
// 需要注意：@Inherited继承只能发生在类上，而不能发生在接口上（也就是说标注在接口上仍然是不能被继承的）
@Inherited
@interface MyAnno {

}
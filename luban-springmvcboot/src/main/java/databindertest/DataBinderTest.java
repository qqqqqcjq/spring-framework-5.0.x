package databindertest;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.validation.BindException;
import org.springframework.validation.DataBinder;

import javax.servlet.annotation.WebServlet;
import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * @date 2020/6/1 22:38
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
@WebServlet
public class DataBinderTest {
    public static void main(String[] args) throws BindException {
        Person person = new Person();
        //构造DataBinder 对象 binder , DataBinder构造函数中需要传入要给那个对象实例绑定属性
        DataBinder binder = new DataBinder(person, "person");
        MutablePropertyValues pvs = new MutablePropertyValues();
        pvs.add("name", "fsx");
        pvs.add("age", 18);

        //调用binder.bind()进行绑定，到传入要绑定的MutablePropertyValues(里面保存了PropertyValue，PropertyValue保存了属性已经属性值)
        binder.bind(pvs);
        Map<?, ?> close = binder.close();

        System.out.println(person);
        System.out.println(close);

    }
}
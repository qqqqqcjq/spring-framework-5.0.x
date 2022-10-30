package springmybatisdemo;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @date 2020/1/14 15:23
 * @author chengjiaqing
 * @version : 0.1
 */

  
public class Test {

    public static void main(String[] args) {
        org.apache.ibatis.logging.LogFactory.useLog4JLogging();
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(JavaConfig.class);
        UserService userService =  annotationConfigApplicationContext.getBean(UserService.class);
        System.out.println(userService.updateUser());
        System.out.println(userService.queryUser());


    }

    public static void testType(){
        List<Integer> list = new ArrayList<Integer>();
        Map<Integer, String> map = new HashMap<Integer, String>();
        System.out.println(Arrays.toString(list.getClass().getTypeParameters()));
        System.out.println(Arrays.toString(map.getClass().getTypeParameters()));

        Map<String, Integer> map1 = new HashMap<String, Integer>() {};
        Type type = map1.getClass().getGenericSuperclass();
        ParameterizedType parameterizedType = (ParameterizedType) type;
        for (Type typeArgument : parameterizedType.getActualTypeArguments()) {
            System.out.println(typeArgument.getTypeName());
        }
    }
}
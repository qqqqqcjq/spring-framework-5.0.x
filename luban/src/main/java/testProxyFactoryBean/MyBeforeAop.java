package testProxyFactoryBean;

import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @date 2020/10/10 17:15
 * @author chengjiaqing
 * @version : 0.1
 */


@Component
public class MyBeforeAop implements MethodBeforeAdvice {

    public void before(Method method, Object[] args, Object target) throws Throwable {
        System.out.println("before aop ["+method.getName()+"] do sth...................");
    }
}
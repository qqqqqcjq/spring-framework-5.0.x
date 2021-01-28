package simulate_mapperscan;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @date 2019/12/29 23:52
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  public class MyInvocationHandler implements InvocationHandler {
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    System.out.println("proxy");
    Method method1 = proxy.getClass().getInterfaces()[0].getMethod(method.getName(),String.class);

    System.out.println(method1.getAnnotation(Select.class).value());
    return null;
  }
}
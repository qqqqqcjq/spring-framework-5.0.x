package maintest.importselector.aop.or.enable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @date 2020/1/2 15:31
 * @author chengjiaqing
 * @version : 0.1
 */ 

  public class MyInvocationhandle implements InvocationHandler {

    private Object bean;

    public MyInvocationhandle(Object bean) {
        this.bean = bean;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if(proxy instanceof  IndexDao){
        System.out.println("proxy indexdaoimpl query");
        method.invoke(bean,args);
      } else if(method.getClass().getSimpleName().equals("others")){
        //others
      }

      return null;
  }
}
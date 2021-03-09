package maintest.importselector.aop.or.enable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;


import java.lang.reflect.Proxy;

/**
 * @date 2020/1/2 15:29
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  public class AopPostProcessor implements BeanPostProcessor {

  //传的参数bean是目标对象，然后返回代理对象
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    if("indexDaoImpl".equals(beanName)) {
      return Proxy.newProxyInstance(this.getClass().getClassLoader(),new Class[]{IndexDao.class},new MyInvocationhandle(bean));

    } else if("others".equals(beanName)){
      //others
      return null;
    }
    return null;
  }

  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    return null;
  }
}
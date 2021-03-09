package simulate_mapperscan;

import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.Proxy;

/**
 * @date 2019/12/29 23:46
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
public class MyFactoryBean implements FactoryBean {

  private Class clazz;

  public MyFactoryBean(Class clazz) {
    this.clazz = clazz;
  }

  public Object getObject() throws Exception {

    Object proxy = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{clazz},
            new MyInvocationHandler());
    return proxy;
  }

  public Class<?> getObjectType() {
    return clazz;
  }


}
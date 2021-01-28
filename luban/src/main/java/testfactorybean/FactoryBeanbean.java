package testfactorybean;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

/**
 * @date 2019/12/16 17:17
 * @author chengjiaqing
 * @version : 0.1
 */ 

@Component("factoryBeanbean")
  public class FactoryBeanbean implements FactoryBean {


	public Object getObject() throws Exception {
		return new FactoryBeanDao();
	}


	public Class<?> getObjectType() {
		return null;
	}

	public void query(){
		System.out.println("FactoryBeanbean query");
	}
}
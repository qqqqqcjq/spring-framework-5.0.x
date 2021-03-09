package comjiaqing;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.LinkedList;
import java.util.List;

/**
 * @date 2019/12/7 14:56
 * @author chengjiaqing
 * @version : 0.1
 */
 @Repository
 @Scope("prototype")
 @DependsOn("manager")
 @Entity(value=  "select * from highfinanceclient")
 @Import(java.util.HashMap.class)
  public class IndexDaoImpl implements IndexDao, InitializingBean, DisposableBean {
	public IndexDaoImpl() {
		System.out.println("indexdaoimpl");
	}

	String str;

	public String getStr() {
		return str;
	}

	public void setStr(String str) {
		this.str = str;
	}


	public void test() {
		System.out.println(this.hashCode());
	}


	public void destroy() throws Exception {
		System.out.println("indexdaodestory");
	}

	@Bean
    public List getList(){
	    return new LinkedList<>();
    }


	public void afterPropertiesSet() throws Exception {
		System.out.println("indexdaoafterinit");

	}
}
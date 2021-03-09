package lubanaop;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

/**
 * @date 2019/12/10 15:17
 * @author chengjiaqing
 * @version : 0.1
 */ 
 @Repository
  public class IndexDao implements Dao {
	public void query() {
        System.out.println("default constructor");
	}

	@Autowired
    OrderDao orderDao;

	//@AddAop
  	public  void query( String args){
		System.out.println("args" + this.hashCode());
	}
}
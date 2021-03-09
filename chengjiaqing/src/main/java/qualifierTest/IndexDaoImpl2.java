package qualifierTest;

import comjiaqing.IndexDao;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

/**
 * @date 2019/12/8 17:19
 * @author chengjiaqing
 * @version : 0.1
 */ 
 @Repository("main")
 @Primary

  public class IndexDaoImpl2 implements IndexDao {

	public void test() {
		System.out.println("indexdaoimpl2");
	}
}
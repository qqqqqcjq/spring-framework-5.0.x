package maintest.importselector.aop.or.enable;

import org.springframework.stereotype.Component;

/**
 * @date 2020/1/2 15:25
 * @author chengjiaqing
 * @version : 0.1
 */ 
 @Component
 public class IndexDaoImpl implements IndexDao {
  public String query() {
    System.out.println("real indexdaoimpl query");
    return null;
  }
}
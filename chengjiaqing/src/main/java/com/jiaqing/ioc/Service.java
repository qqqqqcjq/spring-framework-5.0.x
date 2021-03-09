package com.jiaqing.ioc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @date 2019/12/20 12:24
 * @author chengjiaqing
 * @version : 0.1
 */ 
 @org.springframework.stereotype.Service
  public class Service {
 	@Autowired
	@Qualifier("indexdao")
 	public  IndexDao indexDao;

 	public void query(){
 		indexDao.query();
	}
}
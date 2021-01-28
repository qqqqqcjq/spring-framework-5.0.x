package com.luban.ioc;

import org.springframework.stereotype.Repository;

/**
 * @date 2019/12/20 12:23
 * @author chengjiaqing
 * @version : 0.1
 */ 
 @Repository("indexdao")
  public class IndexDao {

 	public void query(){
		System.out.println("index query");
	}
}
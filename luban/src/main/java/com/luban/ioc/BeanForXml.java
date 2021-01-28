package com.luban.ioc;
/** 
 * @date 2020/3/14 16:10
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class BeanForXml {

    private  IndexDao dao;




    public void setDao(IndexDao dao) {
        this.dao = dao;
    }

    public BeanForXml() {
    }
    public BeanForXml(IndexDao dao) {
        this.dao = dao;
    }

}
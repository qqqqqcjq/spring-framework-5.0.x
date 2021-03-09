package qualifierTest;

import comjiaqing.IndexDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * @date 2019/12/7 14:57
 * @author chengjiaqing
 * @version : 0.1
 */ 
 @Service("indexService")
  public  class IndexService  {//implements ApplicationContextAware

 	private ApplicationContext applicationContext;



	@Autowired
	@Qualifier("main")
  	private IndexDao indexDao;

	public void setIndexDao(IndexDao custom_indexDao) {
		this.indexDao = indexDao;
	}

	public void service(){
		//indexDao = (IndexDao) applicationContext.getBean("indexDao");

		indexDao.test();
	}

	public IndexService(IndexDao indexDao) {
		this.indexDao = indexDao;
	}

//	@Override
//	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
//		this.applicationContext = applicationContext;
//	}
}


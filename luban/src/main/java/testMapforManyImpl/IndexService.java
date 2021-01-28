package testMapforManyImpl;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @date 2020/4/16 15:51
 * @author chengjiaqing
 * @version : 0.1
 */ 


@Service
public class IndexService {

    @Autowired
    private Map<String,IndexDao> indexDaos;

    private IndexDao indexDao;
    private  String indexDaoName;

    public String getIndexDaoName() {
        return indexDaoName;
    }

    public void setIndexDaoName(String indexDaoName) {
        this.indexDaoName = indexDaoName;
    }

    public IndexDao getIndexDao() {
        indexDao = indexDaos.get(indexDaoName);
        return indexDao;
    }


}

//
//@Service
//public class IndexService implements ApplicationContextAware{
//
//    private  ApplicationContext applicationContext;
//    @Override
//    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
//        this.applicationContext = applicationContext;
//    }
//
//    private IndexDao indexDao;
//    private  String indexDaoName;
//
//    public String getIndexDaoName() {
//        return indexDaoName;
//    }
//
//    public void setIndexDaoName(String indexDaoName) {
//        this.indexDaoName = indexDaoName;
//    }
//
//    public IndexDao getIndexDao() {
//        indexDao = (IndexDao)applicationContext.getBean(indexDaoName);
//        return indexDao;
//    }
//}
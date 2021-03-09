package SimpleIOC;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @date 2020/8/18 17:58
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
@Component
public class IndexService {
    @Autowired
    IndexDao indexDao;

    public IndexDao getIndexDao() {
        return indexDao;
    }

    public void setIndexDao(IndexDao indexDao) {
        this.indexDao = indexDao;
    }
}
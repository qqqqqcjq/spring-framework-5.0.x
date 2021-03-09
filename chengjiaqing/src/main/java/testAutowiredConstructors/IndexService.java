package testAutowiredConstructors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @date 2020/8/18 17:58
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
@Component
public class IndexService {

    IndexDao indexDao;
    @Autowired
    public IndexService(IndexDao indexDao) {
        this.indexDao = indexDao;
    }
}
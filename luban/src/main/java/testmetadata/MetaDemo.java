package testmetadata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.HashMap;

/**
 * @date 2020/10/3 15:37
 * @author chengjiaqing
 * @version : 0.1
 */


@Repository("repositoryName")
@Service("serviceName")
@EnableAsync
class MetaDemo extends HashMap<String, String> implements Serializable {
    private static class InnerClass {
    }

    @Autowired
    private String getName() {
        return "demo";
    }
}
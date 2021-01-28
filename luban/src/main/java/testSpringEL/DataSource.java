package testSpringEL;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @date 2020/10/12 23:36
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
@Component
public class DataSource {
    @Value("#{'mysql:jdbc:127'}")
    String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
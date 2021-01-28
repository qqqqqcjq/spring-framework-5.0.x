package SimpleIOC;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;

/**
 * @date 2020/8/18 17:58
 * @author chengjiaqing
 * @version : 0.1
 */


@Configuration
@ComponentScan(basePackages = "SimpleIOC")
public class JavaConfig {

    @Bean
    public BeanMethodPVS beanMethodPVS(){
        return new BeanMethodPVS();
    }
}
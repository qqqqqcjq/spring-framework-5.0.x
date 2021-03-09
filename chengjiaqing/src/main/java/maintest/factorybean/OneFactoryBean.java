package maintest.factorybean;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @date 2020/7/7 11:04
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
@Component
public class OneFactoryBean implements FactoryBean {
    @Override
    public Object getObject() throws Exception {
        return new Date();
    }

    @Override
    public Class<?> getObjectType() {
        return Date.class;
    }
}
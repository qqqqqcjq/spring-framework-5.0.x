package testCircleRefrence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @date 2021/2/1 11:00
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
@Component
public class ComponentA {

    @Autowired
    private ComponentB componentB;
}
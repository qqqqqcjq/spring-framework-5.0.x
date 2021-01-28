package maintest.dependson;

import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * @date 2020/7/9 14:39
 * @author chengjiaqing
 * @version : 0.1
 */

@Component
@DependsOn("beanB")
public class BeanA {
}
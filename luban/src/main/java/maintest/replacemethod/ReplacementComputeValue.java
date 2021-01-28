package maintest.replacemethod;

import org.springframework.beans.factory.support.MethodReplacer;
import java.lang.reflect.Method;

/**
 * @date 2020/4/10 17:50
 * @author chengjiaqing
 * @version : 0.1
 */


public class ReplacementComputeValue implements MethodReplacer {

    public Object reimplement(Object o, Method m, Object[] args) throws Throwable {
        // get the input value, work with it, and return a computed result
        String input = (String) args[0];
        return args[0].toString();
    }
}
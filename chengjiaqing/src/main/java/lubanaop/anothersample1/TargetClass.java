package lubanaop.anothersample1;
/** 
 * @date 2021/1/30 13:41
 * @author chengjiaqing
 * @version : 0.1
 */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 被代理对象
 */
@Component
public class TargetClass {

    @Autowired
    private Dao dao;

    /**
     * 拼接两个字符串
     */
    public String joint(String str1, String str2) {
        System.out.println("执行目标方法 " + dao.dao);
        System.out.println("执行目标方法 " + str1 + "+" + str2);
        return str1 + "+" + str2;
    }


    public final String jointfinal(String str1, String str2) {
        System.out.println("执行目标方法 " + dao.dao);
        System.out.println("执行目标方法 " + str1 + "+" + str2);
        return str1 + "+" + str2;
    }

    private void jointprivate() {
        System.out.println("执行目标方法 " + dao.dao);
    }
}
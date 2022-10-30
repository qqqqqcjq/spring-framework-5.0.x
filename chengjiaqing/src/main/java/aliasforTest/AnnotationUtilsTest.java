package aliasforTest;

import org.junit.Test;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.StringUtils;

/**
 * @date 2022/5/27 11:33
 * @author chengjiaqing
 * @version : 0.1
 */


@ContextConfiguration(value = "aa.xml", locations = "bb.xml")
public class AnnotationUtilsTest {
    @Test
    public void testAliasfor() {
        //ContextConfiguration cc  得到的是一个代理对象
        ContextConfiguration cc = AnnotationUtils.findAnnotation(getClass(), ContextConfiguration.class);
        cc.locations();
        System.out.println(StringUtils.arrayToCommaDelimitedString(cc.locations()));
        System.out.println(StringUtils.arrayToCommaDelimitedString(cc.value()));
    }
}
package comjiaqing;

import java.lang.annotation.*;

/**
 * @author chengjiaqing
 * @version : 0.1
 * @date 2019/12/9 10:32
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Entity {

	String value();
}

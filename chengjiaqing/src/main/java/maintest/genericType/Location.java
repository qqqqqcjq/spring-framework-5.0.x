package maintest.genericType;

import java.lang.annotation.*;

/**
 * @author chengjiaqing
 * @version : 0.1
 * @date 2022/7/19 17:04
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD,
        ElementType.FIELD,
        ElementType.PARAMETER,
        ElementType.ANNOTATION_TYPE,
        ElementType.LOCAL_VARIABLE,
        ElementType.CONSTRUCTOR,
        ElementType.TYPE_USE,
        ElementType.TYPE_PARAMETER,
        ElementType.PACKAGE,
        ElementType.TYPE,
})
public @interface Location {
    String value() default  "";
}

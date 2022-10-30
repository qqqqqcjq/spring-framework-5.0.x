package simulate_mapperscan;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @date 2019/12/29 20:11
 * @author chengjiaqing
 * @version : 0.1
 */ 

@Retention(RetentionPolicy.RUNTIME)
  public @interface Select {
    String value();
}
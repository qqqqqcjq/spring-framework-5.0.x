package simulate_mapperscan;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @date 2019/12/30 0:17
 * @author chengjiaqing
 * @version : 0.1
 */
@Import(MyImpotBeanDefinitionRgister.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface LuBanMapperScan {
}
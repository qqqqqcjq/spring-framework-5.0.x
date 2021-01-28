package maintest.really.aspectj;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.EnableLoadTimeWeaving;

/**
 * @date 2020/7/7 23:52
 * @author chengjiaqing
 * @version : 0.1
 */

@EnableLoadTimeWeaving(aspectjWeaving= EnableLoadTimeWeaving.AspectJWeaving.AUTODETECT)
@EnableAspectJAutoProxy
@ComponentScan
public class Javaconfig {
}
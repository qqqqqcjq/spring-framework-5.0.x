package maintest.importselector.aop.or.enable;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @date 2020/1/2 15:56
 * @author chengjiaqing
 * @version : 0.1
 */ 
 @Retention(RetentionPolicy.RUNTIME)
 @Import(MyImportSelector.class)
 public @interface EnableMyAop {
}
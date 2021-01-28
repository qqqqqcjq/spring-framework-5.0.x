package maintest.importselector.aop.or.enable;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @date 2020/1/2 15:58
 * @author chengjiaqing
 * @version : 0.1
 */ 

//selectImports返回的类名会被注册到bdmap中
  public class MyImportSelector implements ImportSelector {
  public String[] selectImports(AnnotationMetadata annotationMetadata) {
    return new String[]{AopPostProcessor.class.getName()};
  }
}
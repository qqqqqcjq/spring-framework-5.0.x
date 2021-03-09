package simulate_mapperscan;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @date 2019/12/29 20:04
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
public class MyImpotBeanDefinitionRgister implements ImportBeanDefinitionRegistrar {
  public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {

      /*
      因为CardDao是一个接口，所以这样得到的bd,在实例化的时候会报错, 所以我们还需要借助FactoryBean和JDK动态代理
      BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(CardDao.class);
      GenericBeanDefinition gbd = (GenericBeanDefinition) beanDefinitionBuilder.getBeanDefinition();
      beanDefinitionRegistry.registerBeanDefinition("cardDao",gbd);
       */

      BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(MyFactoryBean.class);
      GenericBeanDefinition gbd = (GenericBeanDefinition) beanDefinitionBuilder.getBeanDefinition();
      //gbd对应的类没有默认构造方法了，下面的代码可以让spring通过有参数的构造方法实例化bean
      gbd.getConstructorArgumentValues().addGenericArgumentValue("CardDao");
      beanDefinitionRegistry.registerBeanDefinition("cardDao",gbd);

      BeanDefinitionBuilder beanDefinitionBuilder1 = BeanDefinitionBuilder.genericBeanDefinition(MyFactoryBean.class);
      GenericBeanDefinition gbd1 = (GenericBeanDefinition) beanDefinitionBuilder1.getBeanDefinition();
      //gbd对应的类没有默认构造方法了，下面的代码可以让spring通过有参数的构造方法实例化bean
      gbd1.getConstructorArgumentValues().addGenericArgumentValue("CardDao1");
      beanDefinitionRegistry.registerBeanDefinition("cardDao1",gbd);
  }
}
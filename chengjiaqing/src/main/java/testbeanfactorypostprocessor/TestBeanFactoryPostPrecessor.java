package testbeanfactorypostprocessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

/**
 * @date 2019/12/18 17:18
 * @author chengjiaqing
 * @version : 0.1
 */ 
 @Component
  public class TestBeanFactoryPostPrecessor implements BeanFactoryPostProcessor {

     @Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

		AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) beanFactory.getBeanDefinition("testBeanFactoryPostProcessorBean");
		annotatedBeanDefinition.setScope(ConfigurableBeanFactory.SCOPE_PROTOTYPE);

	}
}
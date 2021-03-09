package testbeannamegenerator;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;

/**
 * @date 2019/12/7 19:58
 * @author chengjiaqing
 * @version : 0.1
 */

//指定扫描路径是指定使用哪个beannamegenerator
public class MyBeanNameGenerator extends AnnotationBeanNameGenerator {

	@Override
	public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
		System.out.println("111");
		return "custom_" + super.generateBeanName(definition, registry);
	}
}
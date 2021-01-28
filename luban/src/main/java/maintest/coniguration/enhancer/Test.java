package maintest.coniguration.enhancer;

import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.lang.reflect.Method;

/**
 * @date 2020/1/3 17:24
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  public class Test {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(JavaConfig.class);
		annotationConfigApplicationContext.getBean("indexDaoImpl1");
		annotationConfigApplicationContext.getBean("indexDaoImpl2");


		//test cglib 代理 IndexDaoImpl1
		Enhancer enhancer = new Enhancer();
		//增强父类，地球人都知道cglib是基于继承来的
		enhancer.setSuperclass(IndexDaoImpl1.class);
		//不继承Factory接口,有空研究，不重要
		enhancer.setUseFactory(false);
		//名字生成策略
		enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);

		//setCallback 和 setCallbackFilter作用是一样的
		enhancer.setCallback(new TestMethodInterceptor());

		IndexDaoImpl1 proxy = (IndexDaoImpl1) enhancer.create();
		proxy.query("hehe");
	}
}

//MethodInterceptor 方法拦截器 这个是cglib包下面的类
class TestMethodInterceptor implements MethodInterceptor {

	@Override
	//o表示代理对象
	//method需要拦截的方法
	//objects表示方法的参数
	//methodProxy是cglib包的类
	public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
		System.out.println("methodProxy");
		methodProxy.invokeSuper(o,objects);//o是代理对象  invokesuper则会使用其父类，也就是目标对象调用这个方法
		return null;
	}
}
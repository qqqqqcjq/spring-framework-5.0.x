package lubanaop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @date 2019/12/10 14:27
 * @author chengjiaqing
 * @version : 0.1
 */ 
@Component
@Aspect //@Aspect("perthis(this(lubanaop.IndexDao))")
////@Scope("prototype")
  public class AspectJClass {

//	@DeclareParents(value="lubanaop.*", defaultImpl=IndexDao.class)
//	public static Dao dao;



//	@Pointcut(" execution(* lubanaop.*.*(..))")
// 	public void qiedian(){
//	}

//	@Pointcut(" this(lubanaop.IndexDao)")
//	public void qiedian(){
//	}

	@Pointcut(" target(lubanaop.IndexDao)")
	public void qiedian(){
	}

//	@Pointcut("@annotation(lubanaop.AddAop)")
//	public void qiedian(){
//	}

//	@Pointcut("args(java.lang.String)")
//	public void qiedian(){ // 切点方法只是为了标记连接点，并不会执行
//	}

//	@Around("qiedian()")
//	public void aroundtest(ProceedingJoinPoint proceedingJoinPoint){
//		Object args[] = proceedingJoinPoint.getArgs();
//		for (int i = 0; i < args.length; i++) {
//			args[i] = args[i] + " word";
//		}
//		try {
//			proceedingJoinPoint.proceed(args);
//		} catch (Throwable throwable) {
//			throwable.printStackTrace();
//		}
//
//		System.out.println("around change the args sucess");
//	}

	@Before("qiedian()")
	public void tongzhibefore(){
		System.out.println("before");
	}


}
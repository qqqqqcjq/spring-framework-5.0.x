package lubanaop.anothersample2;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

/**
 * @date 2021/2/13 12:43
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
@Aspect
public class AopAdviceConfig {

    @Before("execution(* lubanaop.anothersample2.*.*(..))")
    public void beforeAdvice(JoinPoint joinPoint) {
        System.out.println(joinPoint.getThis());
        System.out.println("我是前置通知....");
    }
}
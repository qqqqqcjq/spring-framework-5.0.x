package maintest.really.aspectj;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

/**
 * @date 2020/7/7 23:51
 * @author chengjiaqing
 * @version : 0.1
 */


@Aspect
@Component
public class ProfileAspect {
    @Around("profileMethod()")
    public Object profile(ProceedingJoinPoint pjp) throws Throwable {
        StopWatch sw = new StopWatch(getClass().getName());
        try {
            sw.start(pjp.getSignature().getName());
            return pjp.proceed();
        } finally {
            sw.stop();
            System.err.println(sw.prettyPrint());
        }
    }
    @Pointcut("execution(public * maintest.really.aspectj.*.*(..))")
    public void profileMethod() {
        System.out.println("profileMethod..");
    }
}
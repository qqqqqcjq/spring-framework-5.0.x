package maintest.really.aspectj;

import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @date 2020/7/7 23:50
 * @author chengjiaqing
 * @version : 0.1
 */

@Component
public class DemoBean {
    public void run1() {
        System.out.println("run1...");
    }
    public void run2() throws Exception {
        TimeUnit.SECONDS.sleep(2);
        System.out.println("run2...");
    }
}
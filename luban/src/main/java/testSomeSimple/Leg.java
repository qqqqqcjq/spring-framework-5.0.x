package testSomeSimple;

import org.springframework.stereotype.Component;

/**
 * @date 2020/10/9 14:00
 * @author chengjiaqing
 * @version : 0.1
 */
@Component
public class Leg{
    public void walk() {
        System.out.println("walk a walk");
    }
}
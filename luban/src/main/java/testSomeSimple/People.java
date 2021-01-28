package testSomeSimple;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @date 2020/10/9 14:00
 * @author chengjiaqing
 * @version : 0.1
 */
@Component
public class People{

    private  Leg leg;

    @Autowired
    public void setLeg(Leg leg) {
        this.leg = leg;
    }


    People(Leg leg){
        this.leg = leg;
    }
    public void walk(){
        leg.walk();
    }
}
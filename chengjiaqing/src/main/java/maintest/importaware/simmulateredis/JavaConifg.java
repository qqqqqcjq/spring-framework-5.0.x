package maintest.importaware.simmulateredis;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @date 2020/1/22 14:44
 * @author chengjiaqing
 * @version : 0.1
 */ 
 


@Configuration
@ComponentScan(value = "maintest.importaware.simmulateredis")
@EnabelRedissionHttpSessiontest(keeptime = 900,key = "hahah")

public class JavaConifg {
}
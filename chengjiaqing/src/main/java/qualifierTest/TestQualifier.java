package qualifierTest;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @date 2020/9/28 11:05
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class TestQualifier {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(JavaConfig.class);

    }
}
package annotationBaseTranscation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;




@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"classpath:applicationContext.xml"})
@ContextConfiguration(classes = JavaConfig.class)
public class TestUserService {
    @Resource
    UserService1 userService1;
    /**************************Nest事务   ****************************/
    /**
     *  无异常
     * @throws Exception
     */
    @Test
    public void testFun1() throws Exception {
        userService1.fun1();
    }
    /**
     * 内部事务 有异常
     * @throws Exception
     */
    @Test
    public void testFun2() throws Exception {
        try {
            userService1.fun2();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * 内部事务 有异常 try catch
     * @throws Exception
     */
    @Test
    public void testFun2_2() throws Exception {
        userService1.fun2_2();
    }
    /**
     * 外部事务 异常
     * @throws Exception
     */
    @Test
    public void testFun3() throws Exception {
        userService1.fun3();
    }
    
    /***********require_new ***************************************************/
    /**
     * 无异常
     * @throws Exception
     */
    @Test
    public void testFun4() throws Exception {
        userService1.fun4();
    }
    /**
     * 内部事务异常
     * @throws Exception
     */
    @Test
    public void testFun4_2() throws Exception {
        userService1.fun4_2();
    }
    /**
     * 内部事务异常 try catch
     * @throws Exception
     */
    @Test
    public void testFun4_3() throws Exception {
        userService1.fun4_3();
    }
    
    /**
     * 外部事务异常
     * @throws Exception
     */
    @Test
    public void testFun5() throws Exception {
        userService1.fun5();
    }
    
/*****************required*****************************/
    /**
     * 内部事务异常
     * @throws Exception
     */
    @Test
    public void testFun6() throws Exception {
        userService1.fun6();
    }
    /**
     * 内部事务异常 try catch
     * @throws Exception
     */
    @Test
    public void testFun6_2() throws Exception {
        userService1.fun6_2();
    }
}

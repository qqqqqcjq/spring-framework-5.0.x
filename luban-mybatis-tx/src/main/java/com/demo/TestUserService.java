package com.demo;

import com.demo.service.IUserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

/**
 * tx test demo
 * @author lin
 *
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:applicationContext.xml"})
public class TestUserService {
    @Resource
    IUserService userService;
/**************************Nest事务   ****************************/
    /**
     *  无异常
     * @throws Exception
     */
    @Test
    public void testFun1() throws Exception {
        userService.fun1();
    }
    /**
     * 内部事务 有异常
     * @throws Exception
     */
    @Test
    public void testFun2() throws Exception {
        try {
            userService.fun2();
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
        userService.fun2_2();
    }
    /**
     * 外部事务 异常
     * @throws Exception
     */
    @Test
    public void testFun3() throws Exception {
        userService.fun3();
    }
    
    /***********require_new ***************************************************/
    /**
     * 无异常
     * @throws Exception
     */
    @Test
    public void testFun4() throws Exception {
        userService.fun4();
    }
    /**
     * 内部事务异常
     * @throws Exception
     */
    @Test
    public void testFun4_2() throws Exception {
        userService.fun4_2();
    }
    /**
     * 内部事务异常 try catch
     * @throws Exception
     */
    @Test
    public void testFun4_3() throws Exception {
        userService.fun4_3();
    }
    
    /**
     * 外部事务异常
     * @throws Exception
     */
    @Test
    public void testFun5() throws Exception {
        userService.fun5();
    }
    
/*****************required*****************************/
    /**
     * 内部事务异常
     * @throws Exception
     */
    @Test
    public void testFun6() throws Exception {
        userService.fun6();
    }
    /**
     * 内部事务异常 try catch
     * @throws Exception
     */
    @Test
    public void testFun6_2() throws Exception {
        userService.fun6_2();
    }
}

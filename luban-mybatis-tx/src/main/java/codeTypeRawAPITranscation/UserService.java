package codeTypeRawAPITranscation;

import dao.UserDao;
import dto.UserEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

/**
 * @date 2021/2/5 10:23
 * @author chengjiaqing
 * @version : 0.1
 */


@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"classpath:applicationContext.xml"})
@ContextConfiguration(classes = JavaConfig.class)
public class UserService {

    @Autowired
    UserDao userDao;

    public int save(UserEntity userEntity) throws Exception {
        userDao.save(userEntity);
        return userEntity.getId();
    }

    //事务管理器对象 DataSourceTransactionManager实现类
    @Autowired
    private PlatformTransactionManager transactionManager;
    //事务定义对象
    @Autowired
    private TransactionDefinition transactionDefinition;

    @Test
    public void addUser() {

        //==========!!!!!!!!!!!!!!!!!!!!!!!! begin !!!!!!!!!!!!!!!!!!!!!!!=================================
        /**
         * 这种方式我们可以在Spring中引入一个所有TransactionStatus公用的TransactionDefinition
         * 也可以在写CRUD方法时，创建TransactionStatus的时候，自己创建一个只给当前CRUD使用的TransactionDefinition(一个局部变量)
         */
        //==========!!!!!!!!!!!!!!!!!!!!!!!! end   !!!!!!!!!!!!!!!!!!!!!!!=================================


        //开启一个事务,获得一个事务状态对象，就开启了一个事务了
        TransactionStatus transactionStatus = transactionManager.getTransaction(transactionDefinition);

        //下面的Dao的持久化操作以事务的方式封装起来
        try {
             userDao.save(new UserEntity("codeTypeRawAPITranscation"));
        }catch (Exception e){
            //发生异常，调用rollback方法回滚事务
            transactionManager.rollback(transactionStatus);
        }

        //事务的提交
        transactionManager.commit(transactionStatus);
    }
}


package codeTypeTranscationTemplate;


import dao.UserDao;
import dto.UserEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

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

    //==========!!!!!!!!!!!!!!!!!!!!!!!! begin !!!!!!!!!!!!!!!!!!!!!!!=================================
    /**
     * 这种方式我们可以在Spring中引入一个所有TransactionStatus公用的TransactionDefinition，然后使用公共的TransactionTemplate
     * 也可以为当前CRUD方法，创建一个只给当前CRUD使用的TransactionDefinition(一个局部变量)，然后创建新的TransactionTemplate实例(局部变量)
     *
     */
    //==========!!!!!!!!!!!!!!!!!!!!!!!! end   !!!!!!!!!!!!!!!!!!!!!!!=================================
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    public void addUser() {

        //==========!!!!!!!!!!!!!!!!!!!!!!!! begin !!!!!!!!!!!!!!!!!!!!!!!=================================
        /**
         * 这种方式我们可以在Spring中引入一个所有TransactionStatus公用的TransactionDefinition，然后使用公共的TransactionTemplate
         * 也可以为当前CRUD方法，创建一个只给当前CRUD使用的TransactionDefinition(一个局部变量)，然后创建新的TransactionTemplate实例(局部变量)
         *
         */
        //==========!!!!!!!!!!!!!!!!!!!!!!!! end   !!!!!!!!!!!!!!!!!!!!!!!=================================

        transactionTemplate.execute(new TransactionCallback(){
            public Integer doInTransaction(TransactionStatus status) {
                int count = 0;
                try {
                     count = userDao.save(new UserEntity("codeTypeTranscationTemplate"));
                } catch (Exception e) {
                    status.setRollbackOnly();
                    System.out.println("addUser Error!");
                }
                return new Integer(count);
            }
        });
    }
}
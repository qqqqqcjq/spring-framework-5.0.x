package transactionInterceptorUse;

import codeTypeRawAPITranscation.JavaConfig;
import dao.UserDao;
import dto.UserEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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



@Service
public class UserService {

    @Autowired
    UserDao userDao;

    public int save(UserEntity userEntity) throws Exception {
        userDao.save(userEntity);
        return userEntity.getId();
    }



    public void addUser() {

        try {
            userDao.save(new UserEntity("transactionInterceptorUse"));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}


package baseTxSpace;

import dao.UserDao;
import dto.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import java.util.List;

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
            userDao.save(new UserEntity("baseTxSpace"));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public List selectAll(){
        System.out.println(userDao.selectAll());
        return null;
    }
}


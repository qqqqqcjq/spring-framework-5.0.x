package annotationBaseTranscation;


import dto.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import dao.*;


@Service
public class UserService2 {

    @Resource
    UserDao userDao;

    public void funNone() throws Exception {
        save(new UserEntity("UserService2_none"));

    }


    @Transactional(propagation = Propagation.REQUIRED)
    public void funRequire() throws Exception {
        save(new UserEntity("UserService2_require"));

    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void funRequire2() throws Exception {
        save(new UserEntity("UserService2_require2"));
    }

    
    @Transactional(propagation = Propagation.REQUIRED)
    public void funRequireException() throws Exception {
        save(new UserEntity("UserService2_requireException"));
        throwExcp();

    }

    @Transactional(propagation = Propagation.NESTED)
    public void funNest() throws Exception {
        save(new UserEntity("UserService2_nest"));

    }

    
    @Transactional(propagation = Propagation.NESTED)
    public void funNestException() throws Exception {
        save(new UserEntity("UserService2_nestException"));
        throwExcp();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void funRequireNew() throws Exception {
        save(new UserEntity("UserService2_requireNew"));

    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void funRequireNewException() throws Exception {
        save(new UserEntity("UserService2_requireNewException"));
        throwExcp();
    }


    private void throwExcp() throws Exception {
        throw new RuntimeException("UserService2_boom");
    }

    public int save(UserEntity userEntity) throws Exception {
        userDao.save(userEntity);
        return userEntity.getId();
    }
}
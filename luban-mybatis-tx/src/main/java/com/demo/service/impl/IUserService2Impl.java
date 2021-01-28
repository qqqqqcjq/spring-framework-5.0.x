package com.demo.service.impl;

import com.demo.UserEntity;
import com.demo.dao.UserDao;
import com.demo.service.IUserService2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * Created by zhw on 16/3/24.
 */
@Service
public class IUserService2Impl implements IUserService2 {

    @Resource
    UserDao userDao;

    public void funNone() throws Exception {
        save(new UserEntity("IUserService2_none"));

    }


    @Transactional(propagation = Propagation.REQUIRED)
    public void funRequire() throws Exception {
        save(new UserEntity("IUserService2_require"));

    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void funRequire2() throws Exception {
        save(new UserEntity("IUserService2_require2"));

    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void funRequireException() throws Exception {
        save(new UserEntity("IUserService2_requireException"));

        throwExcp();

    }

    @Transactional(propagation = Propagation.NESTED)
    public void funNest() throws Exception {
        save(new UserEntity("IUserService2_nest"));

    }

    @Override
    @Transactional(propagation = Propagation.NESTED)
    public void funNestException() throws Exception {
        save(new UserEntity("IUserService2_nestException"));
        throwExcp();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void funRequireNew() throws Exception {
        save(new UserEntity("IUserService2_requireNew"));

    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void funRequireNewException() throws Exception {
        save(new UserEntity("IUserService2_requireNewException"));
        throwExcp();


    }


    private void throwExcp() throws Exception {
        throw new RuntimeException("IUserService2_boom");
    }

    public int save(UserEntity userEntity) throws Exception {
        userDao.save(userEntity);
        return userEntity.getId();
    }
}
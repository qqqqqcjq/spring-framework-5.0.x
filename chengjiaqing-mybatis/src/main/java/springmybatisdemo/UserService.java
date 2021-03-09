package springmybatisdemo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @date 2020/1/14 15:10
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
@Service
public class UserService  {

    @Autowired
    UserMapper userMapper;

    @Autowired
    UserDao userDao;

    public List queryUser(){
        return userDao.queryUser();
        //return userMapper.queryUser();
    }

    public int updateUser(){
        return userDao.update("testCommit");
    }
}
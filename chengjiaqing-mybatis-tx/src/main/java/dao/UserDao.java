package dao;


import dto.UserEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserDao {

    int save(UserEntity userEntity) throws Exception;
    UserEntity selectById(int id) throws Exception;
    List selectAll();
}
package dao;


import dto.UserEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserDao {

    public int save(UserEntity userEntity) throws Exception;
    public UserEntity selectById(int id) throws Exception;
    public List selectAll();
}
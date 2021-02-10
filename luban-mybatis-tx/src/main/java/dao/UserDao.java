package dao;


import dto.UserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserDao {

    public int save(UserEntity userEntity) throws Exception;
    public UserEntity selectById(int id) throws Exception;
}
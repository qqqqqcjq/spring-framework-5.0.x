package springmybatisdemo;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * @date 2020/1/14 16:04
 * @author chengjiaqing
 * @version : 0.1
 */

//这里有没有Mapper注解都可以， @MapperScan中指定扫描路径就可以了
public interface UserDao {
    @Select("select * from user")
    public List<Map> queryUser();

    @Update("update user set name=#{name}")
    public int update(String name);

}
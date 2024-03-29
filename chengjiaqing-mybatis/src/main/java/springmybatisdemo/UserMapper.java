package springmybatisdemo;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;


/**
 * @date 2020/1/14 15:05
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
//这里有没有Mapper注解都可以， @MapperScan中指定扫描路径就可以了
@CacheNamespace
public interface UserMapper {

    @Select("select host, user, password from user")
    List<Map> queryUser();
}
package luban;

import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * @date 2020/1/14 16:04
 * @author chengjiaqing
 * @version : 0.1
 */

//这里有没有Mapper注解都可以， @MapperScan中指定扫描路径就可以了
public interface UserDao {
    @Select("select host, user, password from user")
    public List<Map> queryUser();
}
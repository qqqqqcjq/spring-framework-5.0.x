package simulate_mapperscan;
/** 
 * @date 2020/4/18 12:24
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public interface CardDao1 {

    @Select("select * from user where userid = #{id}#")
    String query(String id);
}
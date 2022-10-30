package simulate_mapperscan;
/** 
 * @date 2019/12/29 20:07
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  public interface CardDao {

    @Select("select * from user where userid = #{id}#")
    String query(String id);
}
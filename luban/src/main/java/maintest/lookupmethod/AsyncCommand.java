package maintest.lookupmethod;
/** 
 * @date 2020/4/10 10:13
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class AsyncCommand extends  Command{

    public  boolean execute(){
        System.out.println("change the return for AsyncCommand create Command ");
        return true;
    }
}
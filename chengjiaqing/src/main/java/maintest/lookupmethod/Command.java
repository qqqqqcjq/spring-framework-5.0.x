package maintest.lookupmethod;
/** 
 * @date 2020/4/10 9:35
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class Command {

    private String commandState;

    public String getCommandState() {
        return commandState;
    }

    public void setCommandState(String commandState) {
        this.commandState = commandState;
    }

    public boolean execute(){
        return true;
    }

}
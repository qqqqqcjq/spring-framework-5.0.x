package maintest.lookupmethod;
/** 
 * @date 2020/4/10 9:34
 * @author chengjiaqing
 * @version : 0.1
 */



public abstract class CommandManager {
    public Object process(String commandState) {

        Command command = createCommand();
        command.setCommandState(commandState);
        return command.execute();
    }
    protected abstract Command createCommand();
}
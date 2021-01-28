package testFactoryMethod;
/** 
 * @date 2020/11/27 16:15
 * @author chengjiaqing
 * @version : 0.1
 */

import java.util.HashMap;
import java.util.Map;
public class CarInstanceFactory {
    private Map<Integer, Car> map = new HashMap<Integer,Car>();

    public void setMap(Map<Integer, Car> map) {
        this.map = map;
    }

    public CarInstanceFactory(){
    }

    public Car getCar(int id){
        return map.get(id);
    }
}
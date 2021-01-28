package javaReflection;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

/**
 * @date 2020/10/9 9:23
 * @author chengjiaqing
 * @version : 0.1
 */ 
public class  TestFiledSet{

    public static void main(String[] args) {
        try {
            Field carLightField = Car.class.getDeclaredField("carLight");
            Car car = new Car();
            CarLight carLight = new CarLight();
            //private的field必须执行这个方法才可以，要不会报错
            ReflectionUtils.makeAccessible(carLightField);
            carLightField.set(car, carLight);
            System.out.println(car.getCarLight());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
class Car {
    private CarLight carLight;

    public CarLight getCarLight() {
        return carLight;
    }

    public void setCarLight(CarLight carLight) {
        this.carLight = carLight;
    }
}

class CarLight {
}


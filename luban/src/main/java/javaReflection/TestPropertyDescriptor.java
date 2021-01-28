package javaReflection;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @date 2020/10/9 10:01
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class TestPropertyDescriptor {

    public static void main(String[] args) {
        try {
            Class clazz = Class.forName("javaReflection.Price");
            Object obj =  clazz.newInstance();
            Field[] fields = clazz.getDeclaredFields();
            //写数据，即获得写方法（setter方法）给属性赋值
            for(Field f : fields){
                PropertyDescriptor pd = new PropertyDescriptor(f.getName(),clazz);
                Method method = pd.getWriteMethod();
                method.invoke(obj,"100元");
            }
            System.out.println(obj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

class Price {
//JavaBean get/set方法方法名要求属性名：前两个字母要么都大写，要么都小写
//从源码可以看出：使用默认构造函数new PropertyDescriptor(f.getName(),clazz)，会调用NameGenerator.capitalize(propertyName)生成get/set方法的名字
//NameGenerator.capitalize(propertyName)会根据mBuyPrice生成MBuyPrice， 根据这个方法名，去原Class去查找setMBuyPrice, 然后生成一个方法引用对象MethodRef writeMethodRef，作为PropertyDescriptor的属性

//mBuyPrice使用idea 或者 eclipse来生成的set/get方法会变成 ： getmBuyPrice setmBuyPrice
//这样的话运行后会报java.beans.IntrospectionException: Method not found: isMBuyPrice测错误
//这种情况我们就不能使用默认构造函数，使用new PropertyDescriptor(propertyName, getMethod, setMethod)构造函数，我们自己先找出原Class找出get/set方法，然后作为PropertyDescriptor构造函数的参数去构造PropertyDescriptor

//    public String getmBuyPrice() {
//        return mBuyPrice;
//    }
//
//    public void setmBuyPrice(String mBuyPrice) {
//        this.mBuyPrice = mBuyPrice;
//    }

    private String mBuyPrice;


    public String getMBuyPrice() {
        return mBuyPrice;
    }

    public void setMBuyPrice(String mBuyPrice) {
        this.mBuyPrice = mBuyPrice;
    }

    @Override
    public String toString() {
        return "Price{" +
                "mBuyPrice='" + mBuyPrice + '\'' +
                '}';
    }
}
package javaReflection;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @date 2020/10/9 10:30
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class TestPropertyDescriptorUtil {

    public static void main(String[] args) {
//        try {
//            Class clazz = Class.forName("javaReflection.Price1");
//            Object obj =  clazz.newInstance();
//            Field[] fields = clazz.getDeclaredFields();
//            //写数据，即获得写方法（setter方法）给属性赋值
//            for(Field f : fields){
//                PropertyDescriptor pd = new PropertyDescriptor(f.getName(),clazz);
//                Method method = pd.getWriteMethod();
//                method.invoke(obj,"100元");
//            }
//            System.out.println(obj.toString());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


        try {
            Class clazz = Class.forName("javaReflection.Price1");
            Object obj =  clazz.newInstance();
            //写数据，即获得写方法（setter方法）给属性赋值
            PropertyDescriptorUtil.setProperty(obj,"mBuyPrice","100元");
            System.out.println(obj.toString());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }


    }
}

class Price1 {
    private String mBuyPrice;

    public String getmBuyPrice() {
        return mBuyPrice;
    }

    public void setmBuyPrice(String mBuyPrice) {
        this.mBuyPrice = mBuyPrice;
    }
}

class PropertyDescriptorUtil {

    public static PropertyDescriptor getPropertyDescriptor(Class clazz, String propertyName) {
        StringBuffer sb = new StringBuffer();//构建一个可变字符串用来构建方法名称
        Method setMethod = null;
        Method getMethod = null;
        PropertyDescriptor pd = null;
        try {
            Field f = clazz.getDeclaredField(propertyName);//根据字段名来获取字段
            if (f != null) {
                //构建方法的后缀
                sb.append("set" + propertyName);
                //构建set方法
                setMethod = clazz.getDeclaredMethod(sb.toString(), new Class[]{f.getType()});
                sb.delete(0, sb.length());
                sb.append("get" + propertyName);
                //构建get 方法
                getMethod = clazz.getDeclaredMethod(sb.toString(), new Class[]{});
                //构建一个属性描述器 把对应属性 propertyName 的 get 和 set 方法保存到属性描述器中
                pd = new PropertyDescriptor(propertyName, getMethod, setMethod);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return pd;
    }

    public static void setProperty(Object obj, String propertyName, Object value) {
        Class clazz = obj.getClass();//获取对象的类型
        PropertyDescriptor pd = getPropertyDescriptor(clazz, propertyName);//获取 clazz 类型中的 propertyName 的属性描述器
        Method setMethod = pd.getWriteMethod();//从属性描述器中获取 set 方法
        try {
            setMethod.invoke(obj, new Object[]{value});//调用 set 方法将传入的value值保存属性中去
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Object getProperty(Object obj, String propertyName) {
        Class clazz = obj.getClass();//获取对象的类型
        PropertyDescriptor pd = getPropertyDescriptor(clazz, propertyName);//获取 clazz 类型中的 propertyName 的属性描述器
        Method getMethod = pd.getReadMethod();//从属性描述器中获取 get 方法
        Object value = null;
        try {
            value = getMethod.invoke(clazz, new Object[]{});//调用方法获取方法的返回值
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

}
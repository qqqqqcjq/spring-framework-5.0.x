package maintest.genericType;
/** 
 * @date 2022/7/18 16:58
 * @author chengjiaqing
 * @version : 0.1
 */


import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
/**
 * @date 2020/7/24 14:42
 * @author chengjiaqing
 * @version : 0.1
 */
public class TypeDemo<T> {

    /**
     * Class：普通类型
     */
    public List a;
    public void aInfo() throws NoSuchFieldException {
        Field field;
        Type type;

        field = TypeDemo.class.getDeclaredField("a");
        type = field.getGenericType();
        System.out.println("type of a: " + type.getClass());
        //type of a: class java.lang.Class
        System.out.println("typename of a: " + type.getTypeName());
        //typename of a: java.util.List
        System.out.println("");
    }
    /**
     * TypeVariable：类型变量
     */
    public T b;
    public void bInfo() throws NoSuchFieldException {
        Field field;
        Type type;

        field = TypeDemo.class.getDeclaredField("b");
        type = field.getGenericType();
        System.out.println("type of b: " + type.getClass());
        //type of b: class sun.reflect.generics.reflectiveObjects.TypeVariableImpl
        System.out.println("typename of b: " + type.getTypeName());
        //typename of b: T
        System.out.println("");
    }

    /**
     * GenericArrayType：组件类型为类型变量的数组
     */
    public T[] c;
    public void cInfo() throws NoSuchFieldException {
        Field field;
        Type type;

        field = TypeDemo.class.getDeclaredField("c");
        type = field.getGenericType();
        System.out.println("type of c: " + type.getClass());
        //type of c: class sun.reflect.generics.reflectiveObjects.GenericArrayTypeImpl
        System.out.println("typename of c: " + type.getTypeName());
        //typename of c: T[]
        if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            Type t = genericArrayType.getGenericComponentType();
            System.out.println("type of c's component: " + t.getClass());
            //type of c's component: class sun.reflect.generics.reflectiveObjects.TypeVariableImpl
        }
        System.out.println("");
    }
    /**
     * GenericArrayType：组件类型为参数化类型的数组
     */
    public List<?>[] d;
    public void dInfo() throws NoSuchFieldException {
        Field field;
        Type type;

        field = TypeDemo.class.getDeclaredField("d");
        type = field.getGenericType();
        System.out.println("type of d: " + type.getClass());
        //type of d: class sun.reflect.generics.reflectiveObjects.GenericArrayTypeImpl
        System.out.println("typename of d: " + type.getTypeName());
        //typename of d: java.util.List<?>[]
        if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            Type t = genericArrayType.getGenericComponentType();
            System.out.println("type of d's component: " + t.getClass());
            //type of d's component: class sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
        }
        System.out.println("");
    }
    /**
     * ParameterizedType：参数化类型
     * List<? extends Object>携带的"? extedns Object"
     * 即通配符表达式，也就是WildcardType
     */
    public List<? extends Object> e;
    public  void eInfo() throws NoSuchFieldException {
        Field field;
        Type type;

        field = TypeDemo.class.getDeclaredField("e");
        type = field.getGenericType();
        System.out.println("type of e: " + type.getClass());
        //type of e: class sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
        System.out.println("typename of e: " + type.getTypeName());
        //typename of e: java.util.List<?>
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            for (Type actualTypeArg : actualTypeArguments) {
                System.out.println("type of e's component: " + actualTypeArg.getClass());
                //type of e's component: class sun.reflect.generics.reflectiveObjects.WildcardTypeImpl
            }
        }
        System.out.println("");
    }

    public static void main(String[] args) {
        TypeDemo typeDemo = new TypeDemo();
        try {
            typeDemo.aInfo();
            typeDemo.bInfo();
            typeDemo.cInfo();
            typeDemo.dInfo();
            typeDemo.eInfo();
        } catch (NoSuchFieldException ex) {
            ex.printStackTrace();
        }
    }
}

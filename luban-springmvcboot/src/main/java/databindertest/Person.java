package databindertest;
/** 
 * @date 2020/6/1 22:39
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class Person {

    private String name;
    private  int age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "Persion{name"+getName()+", aget="+ getAge()+"}";
    }
}
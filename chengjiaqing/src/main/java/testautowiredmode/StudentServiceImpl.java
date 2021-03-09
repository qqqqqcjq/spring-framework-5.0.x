package testautowiredmode;
/** 
 * @date 2020/11/9 19:28
 * @author chengjiaqing
 * @version : 0.1
 */ 
 


public class StudentServiceImpl {

    private String studentName;

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentName() {
        return studentName;
    }

    public void say(){
        System.out.println("学生的名字是："+studentName);
    }

}
package testautowiredmode;
/** 
 * @date 2020/11/9 19:27
 * @author chengjiaqing
 * @version : 0.1
 */ 


public class TeacherServiceImpl {

    private String teacherName;

    //<bean id="teacherServiceImpl" class="testautowiredmode.TeacherServiceImpl" autowire="no"> 不会自动注入
    //<bean id="teacherServiceImpl" class="testautowiredmode.TeacherServiceImpl" autowire="byName"> 根据studentServiceImpl作为bean name自动注入
    public StudentServiceImpl studentServiceImpl;

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }


    public void setStudentServiceImpl(StudentServiceImpl studentServiceImpl) {
        this.studentServiceImpl = studentServiceImpl;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void say(){
        System.out.println("老师的名字是："+teacherName);
        System.out.println(teacherName+"老师的学生是："+studentServiceImpl.getStudentName());
    }
}
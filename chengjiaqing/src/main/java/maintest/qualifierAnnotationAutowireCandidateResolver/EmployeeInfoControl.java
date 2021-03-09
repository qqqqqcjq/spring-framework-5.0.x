package maintest.qualifierAnnotationAutowireCandidateResolver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;

/**
 * @date 2020/4/9 9:47
 * @author chengjiaqing
 * @version : 0.1
 */


@Controller
public class EmployeeInfoControl {

    EmployeeService employeeService;

    public EmployeeService getEmployeeService() {
        return employeeService;
    }

    @Autowired
    public void setEmployeeService(@Qualifier("a")  EmployeeService paramEmployeeService) {
        this.employeeService = paramEmployeeService;
    }

    @Autowired
    @Qualifier("b")
    EmployeeService employeeService1;



    public EmployeeService getEmployeeService1() {
        return employeeService1;
    }

    public void setEmployeeService1(EmployeeService employeeService) {
        this.employeeService1 = employeeService;
    }
}
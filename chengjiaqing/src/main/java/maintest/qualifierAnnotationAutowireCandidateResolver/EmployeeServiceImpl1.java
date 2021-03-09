package maintest.qualifierAnnotationAutowireCandidateResolver;

import org.springframework.stereotype.Service;

/**
 * @date 2020/4/9 9:46
 * @author chengjiaqing
 * @version : 0.1
 */


@Service("b")
public class EmployeeServiceImpl1 implements EmployeeService {
    public EmployeeDto getEmployeeById(Long id) {
        return new EmployeeDto();
    }
}
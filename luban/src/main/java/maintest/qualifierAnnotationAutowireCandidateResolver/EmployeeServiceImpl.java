package maintest.qualifierAnnotationAutowireCandidateResolver;

import org.springframework.stereotype.Service;

/**
 * @date 2020/4/9 9:45
 * @author chengjiaqing
 * @version : 0.1
 */


@Service("a")
public class EmployeeServiceImpl implements EmployeeService {
    public EmployeeDto getEmployeeById(Long id) {
        return new EmployeeDto();
    }
}
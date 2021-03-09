package maintest.genericTypeAwareAutowireCandidateResolver;

import org.springframework.stereotype.Service;

/**
 * @date 2020/4/8 16:45
 * @author chengjiaqing
 * @version : 0.1
 */


@Service
public class UserService extends BaseService<User> {
}
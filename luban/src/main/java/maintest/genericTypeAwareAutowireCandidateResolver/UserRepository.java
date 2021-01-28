package maintest.genericTypeAwareAutowireCandidateResolver;

import org.springframework.stereotype.Repository;

/**
 * @date 2020/4/8 16:39
 * @author chengjiaqing
 * @version : 0.1
 */


@Repository
public class UserRepository extends BaseRepository<User> {
}
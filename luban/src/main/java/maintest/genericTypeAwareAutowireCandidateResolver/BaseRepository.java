package maintest.genericTypeAwareAutowireCandidateResolver;

import java.io.Serializable;

/**
 * @date 2020/4/8 16:38
 * @author chengjiaqing
 * @version : 0.1
 */


public abstract class BaseRepository<M extends Serializable> {
    public void save(M m) {
        System.out.println("=====repository save:" + m);
    }
}
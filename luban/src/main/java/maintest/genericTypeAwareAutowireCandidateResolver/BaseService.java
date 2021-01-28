package maintest.genericTypeAwareAutowireCandidateResolver;

import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;

/**
 * @date 2020/4/8 16:35
 * @author chengjiaqing
 * @version : 0.1
 */


public abstract class BaseService<M extends Serializable> {
    public BaseRepository<M> getRepository() {
        return repository;
    }

    public void setRepository(BaseRepository<M> repository) {
        this.repository = repository;
    }

    @Autowired
    protected BaseRepository<M> repository;

    public void save(M m) {
        repository.save(m);
    }
}
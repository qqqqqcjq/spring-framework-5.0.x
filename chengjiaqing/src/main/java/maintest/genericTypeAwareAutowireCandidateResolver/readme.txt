泛型定义依赖注入
BaseService中
@Autowired
protected BaseRepository<M> repository;

GenericTypeAwareAutowireCandidateResolver根据真实传入的类型来选择到底注入那个bean
public class UserService extends BaseService<User>
public class OrganizationService extends BaseService<Organization>


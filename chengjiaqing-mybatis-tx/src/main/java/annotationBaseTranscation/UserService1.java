package annotationBaseTranscation;


import dto.UserEntity;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import  dao.*;

@Service
public class UserService1 {

    @Resource
    UserDao userDao;

    @Resource
    UserService2 userService2;

    /**
     * fun1()默认PROPAGATION_REQUIRED
     * funNest() PROPAGATION_NESTED 无异常
     */
    
    @Transactional
    public void fun1() throws Exception {

        //数据库操作
        funNone();
        //调用另一个service的方法
        userService2.funNest();
    }

    /**
     * fun2()默认PROPAGATION_REQUIRED
     * funNestException() PROPAGATION_NESTED 异常
     */
    
    @Transactional
    public void fun2() throws Exception {
        //嵌套事务的使用场景
        funNone();
        //当所调用的方法为NESTED事务,该事务的回滚可以不影响到调用者的事务
        //当然如果没有catch exception,异常抛出,就将触发调用者事务的回滚
        userService2.funNestException();
        // userService2.funRequire();


    }

    /**
     * fun2_2()默认PROPAGATION_REQUIRED
     * funNestException() PROPAGATION_NESTED 异常
     */
    
    @Transactional
    public void fun2_2() throws Exception {
        //嵌套事务的使用场景
        funNone();

        try {
            //当所调用的方法为NESTED事务,该事务的回滚可以不影响到调用者的事务
            //当然如果没有catch exception,异常抛出,就将触发调用者事务的回滚
            userService2.funNestException();
        } catch (Exception e) {
            //do something
        }

        userService2.funRequire();


    }

    /**
     * fun3()默认PROPAGATION_REQUIRED
     * 外部事务异常
     */
    
    @Transactional
    public void fun3() throws Exception {

        funNone();
        //调用的事务为NESTED事务的方法
        userService2.funNest();

        //此时在调用throwExcp处,触发一个unchecked异常
        throwExcp();

        //此时会发现包括调用的userService2.funNest()也被回滚了
        //也就是说,当调用的方法是NESTED事务,该方法抛出异常如果得到了处理(try-catch),那么该方法发生异常不会触发整个方法的回滚
        //而调用的方法出现unchecked异常,却能触发调用的nested事务的回滚.
    }

    
    @Transactional
    public void fun4() throws Exception {

        //数据库操作
        funNone();
        //调用RequireNew类型事务的方法,调用者的异常回滚不会影响到它
        userService2.funRequireNew();
        //数据库操作
        funNone();
    }

    
    @Transactional
    public void fun4_2() throws Exception {
        //1.标志REQUIRES_NEW会新开启事务，外层事务不会影响内部事务的提交/回滚
        //2.标志REQUIRES_NEW的内部事务的异常，会影响外部事务的回滚
        funNone();
        userService2.funRequireNewException();

    }

    
    @Transactional
    public void fun4_3() throws Exception {
        //而REQUIRES_NEW,当被调用后,就相当于暂停(挂起)当前事务,先开启一个新的事务去执行REQUIRES_NEW的方法,如果REQUIRES_NEW中的异常得到了处理
        //那么他将不影响调用者的事务,同时,调用者之后出现了异常,同样也不会影响之前调用的REQUIRES_NEW方法的事务.

        //不会回滚
        funNone();
        try {
            //当异常得到处理
            userService2.funRequireNewException();
        } catch (Exception e) {

        }
    }

    
    @Transactional
    public void fun5() throws Exception {

        //数据库操作
        funNone();
        //调用RequireNew类型事务的方法,调用者的异常回滚不会影响到它
        userService2.funRequireNew();
        //数据库操作
        funNone();

        //抛出unchecked异常,触发回滚
        throwExcp();
    }

    
    @Transactional
    public void fun6() throws Exception {

        funNone();
        userService2.funRequireException();

    }

    
    @Transactional
    public void fun6_2() throws Exception {

        funNone();

        try {
            //fun6_2默认使用REQUIRE
            //userService2.funRequireException()也使用REQUIRE
            //REQUIRE当前有事务，则使用当前事务，当前无事务则创建事务
            //所以fun6_2和userService2.funRequireException()使用同一事务
            //userService2.funRequireException()出现异常后，将当前事务标志位回滚，由于在fun6_2中做了trycatch处理，程序没有终止而是继续往下走，当事务commit时，check状态，
            //发现，需要事务回滚，所以才会出现不可预知的事务异常:因为事务被标志位回滚，所以事务回滚。
            //抛出异常 ： org.springframework.transaction.UnexpectedRollbackException: Transaction rolled back because it has been marked as rollback-only
            userService2.funRequireException();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        funNone();
    }


    
    @Transactional
    public void fun7() throws Exception {

        funRequire();

        try {
            funNestException();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        funRequire();

    }

    
    @Transactional
    public void fun8() throws Exception {
        ((UserService1) AopContext.currentProxy()).funRequire();

        try {
            ((UserService1) AopContext.currentProxy()).funNestException();
        } catch (Exception e) {
            System.out.println(e.getMessage());

        }

        ((UserService1) AopContext.currentProxy()).funRequire();
    }


    //不带事务的方法
    public void funNone() throws Exception {
        save(new UserEntity("IUserService_None"));

    }

    
    public void funNoneException() throws Exception {
        save(new UserEntity("IUserService_NoneException"));
        throwExcp();
    }


    //启动默认事务的方法
    @Transactional(propagation = Propagation.REQUIRED)
    public void funRequire() throws Exception {
        save(new UserEntity("IUserService_Require"));

    }

    //启动默认事务的方法
    @Transactional(propagation = Propagation.REQUIRED)
    public void funRequire2() throws Exception {
        save(new UserEntity("IUserService_Require2"));

    }

    //启动默认事务的方法,抛出RuntimeException
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void funRequireException() throws Exception {
        save(new UserEntity("IUserService_RequireException"));

        throwExcp();

    }

    //启动嵌套事务的方法
    @Transactional(propagation = Propagation.NESTED)
    public void funNest() throws Exception {
        save(new UserEntity("IUserService_Nest"));

    }


    //启动嵌套事务的方法,但会抛出异常
    
    @Transactional(propagation = Propagation.NESTED)
    public void funNestException() throws Exception {
        save(new UserEntity("IUserService_NestException"));
        throwExcp();
    }

    //REQUIRES_NEW事务的方法
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void funRequireNew() throws Exception {
        save(new UserEntity("IUserService_RequireNew"));

    }

    //REQUIRES_NEW事务的方法,但会抛出异常
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void funRequireNewException() throws Exception {
        save(new UserEntity("IUserService_RequireNewException"));
        throwExcp();
    }


    //抛出异常
    private void throwExcp() throws Exception {
        throw new RuntimeException("IUserService_boom");
    }

    //保存数据
    public int save(UserEntity userEntity) throws Exception {
        userDao.save(userEntity);
        UserEntity ue = userDao.selectById(userEntity.getId());
        System.out.println(ue);
        return userEntity.getId();
    }
}
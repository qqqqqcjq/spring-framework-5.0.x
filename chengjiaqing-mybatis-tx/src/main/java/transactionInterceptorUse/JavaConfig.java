package transactionInterceptorUse;

import com.alibaba.druid.pool.DruidDataSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;


@Configuration
@PropertySource("classpath:db.properties")
@ComponentScan("transactionInterceptorUse")
@ComponentScan("dto")
//开启事务注解
@EnableTransactionManagement //以编程的方式操作事务就不需要这个注解了
public class JavaConfig {

    @Bean
    public DruidDataSource getDruidDataSource(@Value("${db.main.url}") String url, @Value("${db.main.username}") String username,@Value("${db.main.password}") String passward,@Value("${db.main.driver}")String driverClassName ){
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setDriverClassName(driverClassName);
        druidDataSource.setUsername(username);
        druidDataSource.setPassword(passward);
        druidDataSource.setUrl(url);

        return druidDataSource;
    }

    @Bean
    public SqlSessionFactoryBean sqlSessionFactoryBean(DataSource dataSource, @Value("${mybatis.path}") String path) throws IOException {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        sqlSessionFactoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(path));

//        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
//        // configuration.setLogImpl(StdOutImpl.class);//标准输出日志
//        //configuration.setLogImpl(NoLoggingImpl.class);// 不输出日志（）
//        configuration.setLogImpl(Log4jImpl.class);
//        configuration.setMapUnderscoreToCamelCase(true);// 开启驼峰命名
//        configuration.setCallSettersOnNulls(true);// 开启在属性为null也调用setter方法

//        sqlSessionFactoryBean.setConfiguration(configuration);

        return sqlSessionFactoryBean;
    }



    @Bean
    public static MapperScannerConfigurer mapperScannerConfigurer(){
        MapperScannerConfigurer mapperScannerConfigurer = new MapperScannerConfigurer();
        mapperScannerConfigurer.setSqlSessionFactoryBeanName("sqlSessionFactoryBean");
        mapperScannerConfigurer.setBasePackage("dao");

        return mapperScannerConfigurer;
    }

    //------------------------------------begin ------------------------------------------------------
    //需要引入TransactionTemplate PlatformTransactionManager和TransactionDefinition
    @Bean
    public PlatformTransactionManager platformTransactionManager(DataSource dataSource){
        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager();
        dataSourceTransactionManager.setDataSource(dataSource);

        return dataSourceTransactionManager;
    }

    @Bean
    public TransactionDefinition transactionDefinition(){
        return  new DefaultTransactionDefinition();
    }


    //我们自己配置规则，生成代理类
    //这种方式我们直接使用TransactionInterceptor ProxyFactoryBean生成代理类，生成的规则我们可以设定，可以细化
    @Bean
    public TransactionInterceptor transactionInterceptor(PlatformTransactionManager platformTransactionManager){
        Properties properties = new Properties();
        //!!!!!!!=======指定save方法的传播属性为PROPAGATION_REQUIRED=======!!!!!!!!
        properties.put("save","PROPAGATION_REQUIRED");
        TransactionInterceptor transactionInterceptor = new TransactionInterceptor(platformTransactionManager,properties);
        return transactionInterceptor;
    }

    @Bean("userServiceProxy")
    public ProxyFactoryBean proxyFactoryBean(){
        ProxyFactoryBean proxyFactoryBean =  new ProxyFactoryBean();
        proxyFactoryBean.setTargetName("userService");
        proxyFactoryBean.setInterceptorNames("transactionInterceptor");

        return proxyFactoryBean;
    }
    //------------------------------------end ------------------------------------------------------
}
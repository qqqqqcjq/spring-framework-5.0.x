package baseTxSpace;

import com.alibaba.druid.pool.DruidDataSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;


@Configuration
@PropertySource("classpath:db.properties")
@ComponentScan("baseTxSpace")
@ComponentScan("dto")
@ImportResource("classpath:baseTxSpaceConfig.xml")
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


    @Bean("transactionManager")
    public PlatformTransactionManager platformTransactionManager(DataSource dataSource){
        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager();
        dataSourceTransactionManager.setDataSource(dataSource);

        return dataSourceTransactionManager;
    }
}
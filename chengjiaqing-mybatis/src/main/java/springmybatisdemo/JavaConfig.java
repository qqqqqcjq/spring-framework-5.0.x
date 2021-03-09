package springmybatisdemo;

import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * @date 2020/1/14 15:02
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
@Configuration
@MapperScan("springmybatisdemo")
@ComponentScan("springmybatisdemo")
public class JavaConfig {

    @Bean
    public DataSource dataSource(){
        DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource();
        driverManagerDataSource.setDriverClassName("com.mysql.jdbc.Driver");
        driverManagerDataSource.setUsername("root");
        driverManagerDataSource.setPassword("123456");
        driverManagerDataSource.setUrl("jdbc:mysql://192.168.2.250:3306/employees?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8&useSSL=false");

        return driverManagerDataSource;
    }

    @Bean
    public SqlSessionFactoryBean sqlSessionFactoryBean(DataSource dataSource){
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);

//        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
//        // configuration.setLogImpl(StdOutImpl.class);//标准输出日志
//        //configuration.setLogImpl(NoLoggingImpl.class);// 不输出日志（）
//        configuration.setLogImpl(Log4jImpl.class);
//        configuration.setMapUnderscoreToCamelCase(true);// 开启驼峰命名
//        configuration.setCallSettersOnNulls(true);// 开启在属性为null也调用setter方法

//        sqlSessionFactoryBean.setConfiguration(configuration);
        return sqlSessionFactoryBean;
    }
}
package annotationBaseTranscation;

import com.alibaba.druid.pool.DruidDataSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * 使用//@ContextConfiguration(locations = {"classpath:applicationContext.xml"})构造容器，配置文件内容如下：
 * <?xml version="1.0" encoding="UTF-8"?>
 * <beans xmlns="http://www.springframework.org/schema/beans"
 *        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *        xmlns:tx="http://www.springframework.org/schema/tx"
 *        xmlns:context="http://www.springframework.org/schema/context"
 *        xsi:schemaLocation="
 * 		http://www.springframework.org/schema/beans
 * 		http://www.springframework.org/schema/beans/spring-beans.xsd
 * 		http://www.springframework.org/schema/tx
 * 		http://www.springframework.org/schema/tx/spring-tx.xsd
 * 		http://www.springframework.org/schema/context
 * 		http://www.springframework.org/schema/context/spring-context.xsd"
 *        default-lazy-init="false">
 *
 *     <context:property-placeholder location="classpath:db.properties" ignore-unresolvable="true"/>
 *
 *     <context:component-scan base-package="annotationBaseTranscation"/>
 *     <context:component-scan base-package="dto"/>
 *
 *
 *     <bean id="mainDataSource" class="com.alibaba.druid.pool.DruidDataSource"
 *           destroy-method="close">
 *         <property name="driverClassName">
 *             <value>${db.main.driver}</value>
 *         </property>
 *         <property name="url">
 *             <value>${db.main.url}</value>
 *         </property>
 *
 *         <property name="username">
 *             <value>${db.main.username}</value>
 *         </property>
 *         <property name="password">
 *             <value>${db.main.password}</value>
 *         </property>
 *     </bean>
 *
 *
 *     <!-- myBatis文件 -->
 *     <bean id="mainSqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
 *         <property name="dataSource" ref="mainDataSource"/>
 *         <!-- 自动扫描entity目录, 省掉Configuration.xml里的手工配置 -->
 *         <property name="mapperLocations" value="classpath:/mapping/*.xml"/>
 *
 *     </bean>
 *
 *     <bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
 *         <property name="basePackage" value="org.demo.dao" />
 *     </bean>
 *
 *     <!-- DAO接口所在，Spring会自动查找类-->
 *     <bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
 *         <property name="basePackage" value="dao"/>
 *         <property name="sqlSessionFactoryBeanName" value="mainSqlSessionFactory"/>
 *
 *     </bean>
 *
 *     <bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
 *         <property name="dataSource" ref="mainDataSource"></property>
 *     </bean>
 *
 *     <!-- Spring 使用 BeanPostProcessor 来处理 Bean 中的标注，下面开启事务注解-->
 *     <tx:annotation-driven transaction-manager="transactionManager" proxy-target-class="true"/>
 * </beans>
 */

/**
 * @date 2021/2/5 13:38
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
@Configuration
@PropertySource("classpath:db.properties")
@ComponentScan("annotationBaseTranscation")
@ComponentScan("dto")
//开启事务注解
@EnableTransactionManagement
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
    public MapperScannerConfigurer mapperScannerConfigurer(){
        MapperScannerConfigurer mapperScannerConfigurer = new MapperScannerConfigurer();
        mapperScannerConfigurer.setSqlSessionFactoryBeanName("sqlSessionFactoryBean");
        mapperScannerConfigurer.setBasePackage("dao");

        return mapperScannerConfigurer;
    }

    //------------------------------------begin ------------------------------------------------------
    //@EnableTransactionManagement会引入切面，横切逻辑中会new TransactionDefinition, 但是不会new PlatformTransactionManager, 所以我们需要自己引入PlatformTransactionManager的实现类
    @Bean
    public PlatformTransactionManager platformTransactionManager(DataSource dataSource){
        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager();
        dataSourceTransactionManager.setDataSource(dataSource);

        return dataSourceTransactionManager;
    }
    //------------------------------------end ------------------------------------------------------

}

/**
 * AbstractFallbackTransactionAttributeSource#attributeCache
 * 参考AbstractFallbackTransactionAttributeSource#getTransactionAttribute(java.lang.reflect.Method, java.lang.Class)方法
 * 从生成代理的流程可以看出，几乎spring管理的每一个bean的每一个方法都会走到这个方法
 * 每一个@Transactional注解的方法创建一个TransactionAttribute(TransactionDefinition),
 * 没有@Transactional标注方法，就this.attributeCache.put(cacheKey, NULL_TRANSACTION_ATTRIBUTE)
 */




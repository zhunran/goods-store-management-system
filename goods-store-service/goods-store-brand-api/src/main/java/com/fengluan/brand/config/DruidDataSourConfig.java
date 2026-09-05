package com.fengluan.brand.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.SQLException;

@Configuration
public class DruidDataSourConfig {
    @Value("${spring.datasource.url}")
    private String url;
    @Value("${spring.datasource.username}")
    private String username;
    @Value("${spring.datasource.password}")
    private String password;
    @Value("${spring.datasource.driver-class-name:com.mysql.jdbc.Driver}")
    private String driverClassName;

    @Bean
    @Primary
    public DataSource dataSource(@Value("${spring.datasource.druid.initial-size:3}") int initialSize,
                                 @Value("${spring.datasource.druid.min-idle:3}") int minIdle,
                                 @Value("${spring.datasource.druid.max-active:10}") int maxActive,
                                 @Value("${spring.datasource.druid.max-wait:60000}") long maxWait,
                                 @Value("${spring.datasource.druid.validation-query:SELECT 1}") String validationQuery,
                                 @Value("${spring.datasource.druid.filter.stat.enabled:true}") boolean statEnabled) throws SQLException {
        DruidDataSource druidDataSource=new DruidDataSource();
        druidDataSource.setUrl(url);
        druidDataSource.setUsername(username);
        druidDataSource.setPassword(password);
        druidDataSource.setDriverClassName(driverClassName);
        druidDataSource.setInitialSize(initialSize);
        druidDataSource.setMinIdle(minIdle);
        druidDataSource.setMaxActive(maxActive);
        druidDataSource.setMaxWait(maxWait);
        druidDataSource.setValidationQuery(validationQuery);
        druidDataSource.setTestOnBorrow(false);
        druidDataSource.setTestOnReturn(false);
        //sql监控（statFilter需要）
        if(statEnabled)
        {
            druidDataSource.addFilters("stat");
        }
        //防火墙（WallFilter）
        druidDataSource.addFilters("wall");
        return druidDataSource;
    }
}

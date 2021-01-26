package com.example.flowable.config;


import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;


/**
 * 在设置了spring.datasource.enable.dynamic 等于true是开启多数据源
 */
@Configuration
@ConditionalOnProperty(name = {"spring.datasource.dynamic.enable"}, matchIfMissing = false, havingValue = "true")
public class DataSourceConfig {

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.druid.flowable")
    public DataSource dataSourceFlowable() {
        return DruidDataSourceBuilder.create().build();
    }

    @Bean("dataSourceSystem")
    @ConfigurationProperties("spring.datasource.druid.system")
    public DataSource dataSourceSystem() {
        return DruidDataSourceBuilder.create().build();
    }

    @Bean("jdbcTemplateSystem")
    JdbcTemplate jdbcTemplateSystem(@Qualifier("dataSourceSystem") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

}

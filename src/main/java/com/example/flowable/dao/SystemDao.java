package com.example.flowable.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @Author: gx.wu
 * @Date: 2021/1/21 15:46
 * @Description: code something to describe this module what it is
 */
@Repository
public class SystemDao {

    @Qualifier("jdbcTemplateSystem")
    @Autowired
    JdbcTemplate jdbcTemplate;


    public int count(){
        String sql = "SELECT COUNT(1) FROM data_temp_tb ";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}

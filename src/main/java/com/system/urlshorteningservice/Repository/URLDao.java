package com.system.urlshorteningservice.Repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class URLDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public boolean CheckIfLongURLExists(String longUrl){
        String sql = "select count(*) from url url0_ where url0_.longurl = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{longUrl}, Long.class) > 0;
    }

}

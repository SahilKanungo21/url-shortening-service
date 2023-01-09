package com.system.urlshorteningservice.Repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class URLDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public boolean CheckIfLongURLExists(String longUrl) {
        String sql = "select count(*) from url url0_ where url0_.longurl = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{longUrl}, Long.class) > 0;
    }

    public Long deleteRecordsAssociateWithLongURL(String longUrl) {
        String sql = "delete from url url0_ where url0_.longurl=?";
        return (long) jdbcTemplate.update(sql, new Object[]{longUrl});
    }

    public long updateLongURL(String newUrl, String existingUrl) {
        String sql = "update url url0_ set url0_.longurl=? where url0_longurl=?";
        return jdbcTemplate.update(sql, new Object[]{newUrl, existingUrl});
    }

}

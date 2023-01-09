package com.system.urlshorteningservice.Configuration;

import com.system.urlshorteningservice.Repository.URLDao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JDBCConfig {
    @Bean
    public URLDao urlDao() {
        return new URLDao();
    }
}

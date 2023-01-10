package com.system.urlshorteningservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class UrlShorteningServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UrlShorteningServiceApplication.class, args);
    }
}

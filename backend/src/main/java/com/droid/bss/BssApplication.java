package com.droid.bss;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class BssApplication {

    public static void main(String[] args) {
        SpringApplication.run(BssApplication.class, args);
    }
}

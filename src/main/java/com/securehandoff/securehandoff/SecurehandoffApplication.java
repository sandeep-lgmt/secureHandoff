package com.securehandoff.securehandoff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SecurehandoffApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurehandoffApplication.class, args);
    }
}

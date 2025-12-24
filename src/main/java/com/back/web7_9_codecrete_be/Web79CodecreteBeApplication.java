package com.back.web7_9_codecrete_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
public class Web79CodecreteBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(Web79CodecreteBeApplication.class, args);
    }
    // 파이팅
}

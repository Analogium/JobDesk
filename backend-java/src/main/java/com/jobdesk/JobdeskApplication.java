package com.jobdesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobdeskApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobdeskApplication.class, args);
    }
}

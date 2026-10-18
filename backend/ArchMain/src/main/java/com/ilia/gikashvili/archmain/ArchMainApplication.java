package com.ilia.gikashvili.archmain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ArchMainApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArchMainApplication.class, args);
    }
}

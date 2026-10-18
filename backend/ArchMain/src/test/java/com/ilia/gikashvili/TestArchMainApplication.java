package com.ilia.gikashvili;

import com.ilia.gikashvili.archmain.ArchMainApplication;
import org.springframework.boot.SpringApplication;

public class TestArchMainApplication {

    public static void main(String[] args) {
        SpringApplication.from(ArchMainApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}

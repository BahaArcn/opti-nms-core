package com.opticoms.optinmscore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OptiNmsCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(OptiNmsCoreApplication.class, args);
    }

}

package com.etri.datalake;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SodasApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SodasApiApplication.class, args);
    }

}

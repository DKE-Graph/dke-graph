package com.etri.sodasapi;

import com.etri.sodasapi.auth.KeycloakConfig;
import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(KeycloakConfig.class)
public class SodasApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SodasApiApplication.class, args);
    }

}

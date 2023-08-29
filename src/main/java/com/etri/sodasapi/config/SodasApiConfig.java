package com.etri.sodasapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class SodasApiConfig {

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info().title("Sodas-Api")
                .version("0.0.1")
                .description("Spring Boot를 이용한 Sodas API입니다.");

        return new OpenAPI()
                .components(new Components())
                .info(info);
    }
}

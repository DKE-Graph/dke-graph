package com.etri.sodasapi.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class KeyCloakComponent {

    @Bean
    Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl("http://keycloak.221.154.134.31.traefik.me:10017/realms/master-i")
                .realm("master-i")
                .clientId("platform")
                .grantType(OAuth2Constants.PASSWORD)
                .username("sodas-api")
                .password("ae46ef1b-f7bc-47db-8096-d821ffb999e1")
                .build();
    }
}

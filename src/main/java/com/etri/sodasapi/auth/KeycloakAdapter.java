package com.etri.sodasapi.auth;

import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.rotation.AdapterTokenVerifier;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KeycloakAdapter {

    private KeycloakConfig keycloakConfig;

    public KeycloakAdapter(){
        this.keycloakConfig = new KeycloakConfig();

        KeycloakConfig.Credentials credentials = new KeycloakConfig.Credentials();
        credentials.setSecret("ae46ef1b-f7bc-47db-8096-d821ffb999e1");
        keycloakConfig.setCredentials(credentials);
        keycloakConfig.setRealm("master-i");
        keycloakConfig.setResource("platform");
        keycloakConfig.setAuthServerUrl("http://keycloak.221.154.134.31.traefik.me:10017/");

    }

    public KeycloakDeployment getKeycloakDeployment() {
        AdapterConfig adapterConfig = new AdapterConfig();
        adapterConfig.setAuthServerUrl(keycloakConfig.getAuthServerUrl());
        adapterConfig.setRealm(keycloakConfig.getRealm());
        adapterConfig.setResource(keycloakConfig.getResource());
        adapterConfig.setCredentials(new HashMap<>());
        adapterConfig.getCredentials().put("secret", keycloakConfig.getCredentials().getSecret());

        return KeycloakDeploymentBuilder.build(adapterConfig);
    }


    public AccessToken verifyToken(String tokenString) {
        KeycloakDeployment deployment = getKeycloakDeployment();

        try {
            System.out.println(getAttribute(AdapterTokenVerifier.verifyToken(tokenString, deployment)));
            return AdapterTokenVerifier.verifyToken(tokenString, deployment);
        } catch (VerificationException e) {
            // 토큰 검증 실패
            e.printStackTrace();
            return null;
        }
    }

    public Map<String, Object> getAttribute(AccessToken accessToken){
        Map<String, Object> attributes = accessToken.getOtherClaims();
        return accessToken.getOtherClaims();
    }

}

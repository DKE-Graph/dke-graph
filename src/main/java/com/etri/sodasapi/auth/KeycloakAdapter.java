package com.etri.sodasapi.auth;

import com.etri.sodasapi.objectstorage.rgw.RGWService;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.rotation.AdapterTokenVerifier;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.naming.event.ObjectChangeListener;

@Component
public class KeycloakAdapter {

    private KeycloakConfig keycloakConfig;
    private RGWService rgwService;

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
        return accessToken.getOtherClaims();
    }

    public Map<String, Object> getUserPk(String token){
        System.out.println(rgwService.getS3Credential("sodas_dev_user"));

        try {
            KeycloakDeployment deployment = getKeycloakDeployment();
            AccessToken accessToken = AdapterTokenVerifier.verifyToken(token, deployment);

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("group", (ArrayList<?>) accessToken.getOtherClaims().get("group"));
            userInfo.put("userId", accessToken.getPreferredUsername());

            return userInfo;
        } catch (VerificationException e){e.printStackTrace();}

        return null;
        //return (String) Jwts.parserBuilder().setSigningKey(keycloakConfig.getCredentials().getSecret()).build().parseClaimsJws(token).getBody().get("sub");
    }

}

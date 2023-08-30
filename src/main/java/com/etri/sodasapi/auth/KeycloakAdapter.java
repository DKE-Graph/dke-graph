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
import org.twonote.rgwadmin4j.model.S3Credential;

import javax.naming.event.ObjectChangeListener;

import static org.apache.http.client.methods.RequestBuilder.put;

@Component
@RequiredArgsConstructor
public class KeycloakAdapter {

    private final KeycloakConfig keycloakConfig;
    private final RGWService rgwService;

//    public KeycloakAdapter() {
//        this(keycloakConfig, rgwService);
//    }

    public KeycloakDeployment getKeycloakDeployment() {
        AdapterConfig adapterConfig = new AdapterConfig();
        adapterConfig.setAuthServerUrl(keycloakConfig.getAuthServerUrl());
        adapterConfig.setRealm(keycloakConfig.getRealm());
        adapterConfig.setResource(keycloakConfig.getResource());

        Map<String, Object> credential =  new HashMap<String, Object>() {
            {
                put("secret", keycloakConfig.getCredentials().getSecret());
            }
        };
        adapterConfig.setCredentials(credential);

        return KeycloakDeploymentBuilder.build(adapterConfig);
    }


    public AccessToken verifyToken(String tokenString) {
        KeycloakDeployment deployment = getKeycloakDeployment();

        try {
            //System.out.println(getAttribute(AdapterTokenVerifier.verifyToken(tokenString, deployment)));
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

            S3Credential s3Credential =  rgwService.getS3Credential(accessToken.getPreferredUsername());
            userInfo.put("accessKey", s3Credential.getAccessKey());
            userInfo.put("secretKey", s3Credential.getSecretKey());

            return userInfo;
        } catch (VerificationException e){e.printStackTrace();}

        return null;
        //return (String) Jwts.parserBuilder().setSigningKey(keycloakConfig.getCredentials().getSecret()).build().parseClaimsJws(token).getBody().get("sub");
    }

}

package com.etri.sodasapi.dashboard;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;

@Service
public class DSService {

    @Value("${MGR_ENDPOINT}")
    private String MGR_ENDPOINT;

    private HttpHeaders headers;


    public void getToken() {
        if(headers.get("Authorization") != null){
            return;
        }

        URI uri = UriComponentsBuilder
                .fromUriString("http://"+MGR_ENDPOINT)
                .path("api/auth")
                .encode()
                .build()
                .toUri();


        HashMap<String, String> requestBody = new HashMap<>();
        requestBody.put("username", "sodas_admin");
        requestBody.put("password", "sodas_admin_secret");

        headers = new HttpHeaders();
        headers.add("Accept", "application/vnd.ceph.api.v1.0+json");
        headers.add("Content-Type", "application/json");

        RequestEntity<HashMap> requestEntity = RequestEntity
                .post(uri)
                .headers(headers)
                .body(requestBody);

        System.out.println(requestBody);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<HashMap> responseEntity = restTemplate.exchange(requestEntity, HashMap.class);

        String token = (String)responseEntity.getBody().get("token");
        headers.add("Authorization", "Bearer " + token);

    }
}

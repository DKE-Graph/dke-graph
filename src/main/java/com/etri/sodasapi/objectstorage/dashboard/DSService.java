package com.etri.sodasapi.objectstorage.dashboard;

import com.etri.sodasapi.objectstorage.common.Quota;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class DSService {

    @Value("${MGR_ENDPOINT}")
    private String MGR_ENDPOINT;

    private HttpHeaders headers;

    public List<HashMap> userQoutaInfo(String userName) {
        getToken();

        URI uri = UriComponentsBuilder
                .fromUriString(MGR_ENDPOINT)
                .path("/api/rgw/user/" + userName+"/quota")
                .encode()
                .build()
                .toUri();

        RequestEntity<Void> requestEntity = RequestEntity
                .get(uri)
                .headers(headers)
                .build();

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<HashMap> responseEntity = restTemplate.exchange(requestEntity, HashMap.class);

        List<HashMap> quotaList = new ArrayList<>();
        quotaList.add((HashMap<String, Object>) responseEntity.getBody().get("bucket_quota"));
        quotaList.add((HashMap<String, Object>) responseEntity.getBody().get("user_quota"));

        return quotaList;
    }

    public void qoutaConfig(String userName, Quota quota) {
        quotaConfigOperation(userName, quota);
    }

    public void qoutaDisable(String userName, String quotaType) {
        System.out.println(quotaType);
        Quota quota = new Quota("false", "0", "0", quotaType);
        quotaConfigOperation(userName, quota);
    }

    public void quotaConfigOperation(String userName, Quota quota){
        getToken();

        URI uri = UriComponentsBuilder
                .fromUriString(MGR_ENDPOINT)
                .path("/api/rgw/user/" + userName + "/quota")
                .encode()
                .build()
                .toUri();


        HashMap<String, String> requestBody = new HashMap<>();
        requestBody.put("enabled", quota.getEnabled());
        requestBody.put("max_objects", quota.getMax_objects());
        requestBody.put("max_size_kb", quota.getMax_size_kb());
        requestBody.put("quota_type", quota.getQuota_type());

        RequestEntity<HashMap> requestEntity = RequestEntity
                .put(uri)
                .headers(headers)
                .body(requestBody);

        RestTemplate restTemplate = new RestTemplate();

        restTemplate.exchange(requestEntity, void.class);
    }

    public synchronized void getToken() {
        if(headers != null){
            return;
        }

        URI uri = UriComponentsBuilder
                .fromUriString(MGR_ENDPOINT)
                .path("/api/auth")
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

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<HashMap> responseEntity = restTemplate.exchange(requestEntity, HashMap.class);

        String token = (String)responseEntity.getBody().get("token");
        headers.add("Authorization", "Bearer " + token);

    }
}

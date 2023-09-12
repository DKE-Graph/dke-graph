package com.etri.datalake.objectstorage.dashboard;

import com.etri.datalake.config.ObjectStorageConfig;
import com.etri.datalake.objectstorage.common.SQuota;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DSService {
    private final ObjectStorageConfig objectStorageConfig;

    private HttpHeaders headers;

    public List<HashMap> userQoutaInfo(String userName) {
        getToken();

        URI uri = UriComponentsBuilder
                .fromUriString(objectStorageConfig.getMgrEndpoint())
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
        quotaList.add((HashMap<String, Object>) responseEntity.getBody().get("user_quota"));

        return quotaList;
    }

    public void qoutaConfig(String userName, SQuota quota) {
        quotaConfigOperation(userName, quota);
    }

    public void qoutaDisable(String userName, String quotaType) {
        System.out.println(quotaType);
        SQuota quota = new SQuota("false", "0", "0", quotaType);
        quotaConfigOperation(userName, quota);
    }

    public void quotaConfigOperation(String userName, SQuota quota){
        getToken();

        URI uri = UriComponentsBuilder
                .fromUriString(objectStorageConfig.getMgrEndpoint())
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
                .fromUriString(objectStorageConfig.getMgrEndpoint())
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

    public List<HashMap> bucketQoutaInfo(String userName) {
        getToken();

        URI uri = UriComponentsBuilder
                .fromUriString(objectStorageConfig.getMgrEndpoint())
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

        return quotaList;
    }
}

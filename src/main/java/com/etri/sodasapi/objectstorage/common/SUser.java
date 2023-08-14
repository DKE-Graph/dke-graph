package com.etri.sodasapi.objectstorage.common;

import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class SUser {
    private String accessKey;
    private String secretKey;
    private String email;
    private String maxBuckets;
    private String displayName;
    private String uid;
}

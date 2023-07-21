package com.etri.sodasapi.common;

import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class Key {
    private String accessKey;
    private String secretKey;

    public Key(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }
}

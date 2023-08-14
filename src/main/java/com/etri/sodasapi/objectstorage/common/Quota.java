package com.etri.sodasapi.objectstorage.common;

import lombok.Data;
import lombok.Getter;

/*
    TODO: 값 유효성 검사 필요
 */
@Data
@Getter
public class Quota {
    private String enabled;
    private String max_objects;
    private String max_size_kb;
    private String quota_type;

    public Quota(String enabled, String max_objects, String max_size_kb, String quota_type) {
        this.enabled = enabled;
        this.max_objects = max_objects;
        this.max_size_kb = max_size_kb;
        this.quota_type = quota_type;
    }
}

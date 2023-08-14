package com.etri.sodasapi.objectstorage.common;

import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class SSubUser {
    private String subUid;
    private String permission;
    private String accessKey;
    private String secretKey;
}

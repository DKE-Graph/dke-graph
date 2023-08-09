package com.etri.sodasapi.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
@Schema(description = "키")
@Data
@Getter
public class Key {
    @Schema(description = "접근키")
    private String accessKey;

    @Schema(description = "비밀키")
    private String secretKey;

    public Key(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }
}

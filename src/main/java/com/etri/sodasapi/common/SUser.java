package com.etri.sodasapi.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
@Schema(description = "유저")
@Data
@Getter
public class SUser {
    @Schema(description = "접근키")
    private String accessKey;

    @Schema(description = "비밀키")
    private String secretKey;

    @Schema(description = "이메일")
    private String email;

    @Schema(description = "최대 버킷수")
    private String maxBuckets;

    @Schema(description = "이름")
    private String displayName;

    @Schema(description = "유저 id")
    private String uid;
}

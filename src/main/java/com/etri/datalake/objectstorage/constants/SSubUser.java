package com.etri.datalake.objectstorage.constants;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;

@Schema(description = "서브 유저")
@Data
@Getter
public class SSubUser {
    @Schema(description = "서브유저 id")
    private String subUid;

    @Schema(description = "권한")
    private String permission;

    @Schema(description = "접근키")
    private String accessKey;

    @Schema(description = "비밀키")
    private String secretKey;
}

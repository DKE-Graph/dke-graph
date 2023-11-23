package com.etri.datalake.objectstorage.constants;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;

@Schema(description = "유저권한")
@Data
@Getter
public class SUserPerm {
    @Schema(description = "유저ID")
    private String userId;

    @Schema(description = "권한")
    private String permission;
}

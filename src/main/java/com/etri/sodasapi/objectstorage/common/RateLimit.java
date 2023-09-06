package com.etri.sodasapi.objectstorage.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;

@Schema(description = "API 호출수 제한")
@Data
@Getter
public class RateLimit {
    @Schema(description = "가능여부")
    private String enabled;

    @Schema(description = "초당 읽을수 있는 바이트")
    private long maxReadBytes;

    @Schema(description = "초당 쓸수 있는 바이트")
    private long maxWriteBytes;

    @Schema(description = "초당 읽을수 있는 호출수")
    private long maxReadOps;

    @Schema(description = "초당 쓸수 있는 호출수")
    private long maxWriteOps;

    public RateLimit(String enabled, long maxReadBytes, long maxWriteBytes, long maxReadOps, long maxWriteOps){
        this.enabled = enabled;
        this.maxReadBytes = maxReadBytes;
        this.maxWriteBytes = maxWriteBytes;
        this.maxReadOps = maxReadOps;
        this.maxWriteOps = maxWriteOps;
    }

}

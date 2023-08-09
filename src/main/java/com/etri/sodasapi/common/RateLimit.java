package com.etri.sodasapi.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;

@Schema(description = "API 호출수 제한")
@Data
@Getter
public class RateLimit {
    @Schema(description = "가능여부")
    private String enabled;

    @Schema(description = "최대로 읽을 수 있는 바이트 용량")
    private long maxReadBytes;

    @Schema(description = "최대로 쓸 수 있는 바이트 용량")
    private long maxWriteBytes;

    private long maxReadOpts;
    private long maxWriteOpts;

    public RateLimit(String enabled, long maxReadBytes, long maxWriteBytes, long maxReadOpts, long maxWriteOpts){
        this.enabled = enabled;
        this.maxReadBytes = maxReadBytes;
        this.maxWriteBytes = maxWriteBytes;
        this.maxReadOpts = maxReadOpts;
        this.maxWriteOpts = maxWriteOpts;
    }

}

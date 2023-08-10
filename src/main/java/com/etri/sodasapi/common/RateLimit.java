package com.etri.sodasapi.common;

import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class RateLimit {

    private String enabled;

    private long maxReadBytes;
    private long maxWriteBytes;
    private long maxReadOps;
    private long maxWriteOps;

    public RateLimit(String enabled, long maxReadBytes, long maxWriteBytes, long maxReadOps, long maxWriteOps){
        this.enabled = enabled;
        this.maxReadBytes = maxReadBytes;
        this.maxWriteBytes = maxWriteBytes;
        this.maxReadOps = maxReadOps;
        this.maxWriteOps = maxWriteOps;
    }
}

package com.etri.sodasapi.common;

import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class RateLimit {

    private String enabled;

    private long maxReadBytes;
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

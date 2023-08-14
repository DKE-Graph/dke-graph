package com.etri.sodasapi.objectstorage.common;

import lombok.Data;
import lombok.Getter;

import java.util.Date;

@Data
@Getter
public class SBucket {
    private String bucketName;
    private Date createDate;

    public SBucket(String bucketName, Date createDate) {
        this.bucketName = bucketName;
        this.createDate = createDate;
    }
}

package com.etri.sodasapi.objectstorage.common;

import lombok.Data;
import lombok.Getter;

import java.util.Date;

@Data
@Getter
public class BObject {
    private String objectName;
    private Long size;
    private Date createTime;

    public BObject(String objectName, Long size, Date createTime) {
        this.objectName = objectName;
        this.size = size;
        this.createTime = createTime;
    }
}

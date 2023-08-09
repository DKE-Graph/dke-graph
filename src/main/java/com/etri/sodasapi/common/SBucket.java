package com.etri.sodasapi.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;

import java.util.Date;
@Schema(description = "S버켓")
@Data
@Getter
public class SBucket {
    @Schema(description = "버켓 이름")
    private String bucketName;

    @Schema(description = "생성된 시간")
    private Date createDate;

    public SBucket(String bucketName, Date createDate) {
        this.bucketName = bucketName;
        this.createDate = createDate;
    }
}

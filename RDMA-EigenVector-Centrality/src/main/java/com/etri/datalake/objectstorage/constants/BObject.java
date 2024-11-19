package com.etri.datalake.objectstorage.constants;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;

import java.util.Date;
@Schema(description = "B오브젝트")
@Data
@Getter
public class BObject {
    @Schema(description = "오브젝트 이름")
    private String objectName;

    @Schema(description = "용량")
    private Long size;

    @Schema(description = "생성된 시간")
    private Date createTime;

    public BObject(String objectName, Long size, Date createTime) {
        this.objectName = objectName;
        this.size = size;
        this.createTime = createTime;
    }
}

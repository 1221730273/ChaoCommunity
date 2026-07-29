package com.ljc.chaocommunity.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "文件上传结果")
public class UploadVO {

    @Schema(description = "文件记录ID")
    private Long fileId;

    @Schema(description = "文件访问URL")
    private String url;
}

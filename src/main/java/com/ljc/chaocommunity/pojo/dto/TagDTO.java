package com.ljc.chaocommunity.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "标签请求（创建/修改共用）")
public class TagDTO {

    @Schema(description = "标签ID（修改时必传）")
    private Long id;

    @NotBlank(message = "标签名称不能为空")
    @Size(max = 10, message = "标签名称最多10个字")
    @Schema(description = "标签名称")
    private String name;

    @Schema(description = "图标文件ID")
    private Long fileId;
}

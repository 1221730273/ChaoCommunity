package com.ljc.chaocommunity.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "封面修改请求")
public class CoverUpdateDTO {

    @NotNull(message = "文件ID不能为空")
    @Schema(description = "新封面文件ID")
    private Long fileId;
}

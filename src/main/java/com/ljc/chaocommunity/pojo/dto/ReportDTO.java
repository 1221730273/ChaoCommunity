package com.ljc.chaocommunity.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "举报请求")
public class ReportDTO {

    @NotNull(message = "目标ID不能为空")
    @Schema(description = "被举报对象ID")
    private Long targetId;

    @NotBlank(message = "举报类型不能为空")
    @Schema(description = "举报类型 POST/COMMENT/USER")
    private String targetType;

    @Schema(description = "举报原因")
    private String reason;
}

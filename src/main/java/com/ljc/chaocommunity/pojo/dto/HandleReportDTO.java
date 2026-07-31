package com.ljc.chaocommunity.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "处理举报请求")
public class HandleReportDTO {

    @NotNull(message = "举报ID不能为空")
    @Schema(description = "举报ID")
    private Long id;

    @NotNull(message = "处理状态不能为空")
    @Schema(description = "处理状态 1=已通过 2=已驳回")
    private Integer status;

    @Schema(description = "处理备注")
    private String handleRemark;
}

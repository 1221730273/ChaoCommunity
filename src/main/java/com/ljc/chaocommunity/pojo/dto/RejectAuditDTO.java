package com.ljc.chaocommunity.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "审核拒绝请求")
public class RejectAuditDTO {

    @NotNull(message = "审核ID不能为空")
    @Schema(description = "审核记录ID")
    private Long id;

    @Schema(description = "拒绝原因")
    private String reason;
}

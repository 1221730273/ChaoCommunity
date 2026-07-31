package com.ljc.chaocommunity.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端举报列表
 */
@Data
@Schema(description = "举报视图")
public class ReportVO {

    @Schema(description = "举报ID")
    private Long id;

    @Schema(description = "举报人ID")
    private Long userId;

    @Schema(description = "举报人用户名")
    private String username;

    @Schema(description = "被举报对象ID")
    private Long targetId;

    @Schema(description = "举报类型")
    private String targetType;

    @Schema(description = "举报原因")
    private String reason;

    @Schema(description = "处理状态 0待处理 1已通过 2已驳回")
    private Integer status;

    @Schema(description = "处理管理员ID")
    private Long handlerId;

    @Schema(description = "处理备注")
    private String handleRemark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

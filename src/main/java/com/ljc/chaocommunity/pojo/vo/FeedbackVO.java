package com.ljc.chaocommunity.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端反馈列表
 */
@Data
@Schema(description = "反馈视图")
public class FeedbackVO {

    @Schema(description = "反馈ID")
    private Long id;

    @Schema(description = "提交人ID")
    private Long userId;

    @Schema(description = "提交人用户名")
    private String username;

    @Schema(description = "反馈类型 BUG/SUGGESTION/OTHER")
    private String type;

    @Schema(description = "反馈内容")
    private String content;

    @Schema(description = "联系方式")
    private String contact;

    @Schema(description = "状态 0=未读 1=已读")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

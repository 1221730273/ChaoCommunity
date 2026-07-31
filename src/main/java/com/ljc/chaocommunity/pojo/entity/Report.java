package com.ljc.chaocommunity.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 举报
 */
@Data
@TableName("report")
public class Report {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 举报人ID */
    private Long userId;

    /** 被举报对象ID */
    private Long targetId;

    /** 举报类型 POST / COMMENT / USER */
    private String targetType;

    /** 举报原因 */
    private String reason;

    /** 处理状态 0=待处理 1=已通过 2=已驳回 */
    private Integer status;

    /** 处理管理员ID */
    private Long handlerId;

    /** 处理备注 */
    private String handleRemark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

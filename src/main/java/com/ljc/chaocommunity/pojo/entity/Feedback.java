package com.ljc.chaocommunity.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户反馈
 */
@Data
@TableName("feedback")
public class Feedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提交人ID */
    private Long userId;

    /** 反馈类型 BUG / SUGGESTION / OTHER */
    private String type;

    /** 反馈内容 */
    private String content;

    /** 联系方式（选填） */
    private String contact;

    /** 状态 0=未读 1=已读 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

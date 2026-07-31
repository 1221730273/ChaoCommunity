package com.ljc.chaocommunity.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子审核表 - 保存用户提交的待审核版本
 */
@Data
@TableName("post_audit")
public class PostAudit {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提交用户ID */
    private Long userId;

    /** 原帖子ID（新建帖子时为NULL） */
    private Long postId;

    /** 帖子标题 */
    private String title;

    /** 帖子内容（Markdown） */
    private String content;

    /** 分类ID */
    private Long categoryId;

    /** 封面文件ID（file_record表主键） */
    private Long coverFileId;

    /** 标签ID逗号分隔（如 "1,2,3"） */
    private String tagIds;

    /** 状态：0待审核 1审核通过 2审核拒绝 */
    private Integer status;

    /** 拒绝原因 */
    private String rejectReason;

    /** 处理人ID（管理员） */
    private Long handlerId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

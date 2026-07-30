package com.ljc.chaocommunity.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论点赞关系
 */
@Data
@TableName("comment_like")
public class CommentLike {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 点赞用户ID */
    private Long userId;

    /** 评论ID */
    private Long commentId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

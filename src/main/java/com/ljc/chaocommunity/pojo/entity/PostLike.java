package com.ljc.chaocommunity.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子点赞关系
 */
@Data
@TableName("post_like")
public class PostLike {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 点赞用户ID */
    private Long userId;

    /** 帖子ID */
    private Long postId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

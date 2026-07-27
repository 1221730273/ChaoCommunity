package com.ljc.chaocommunity.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子-标签关联
 */
@Data
@TableName("post_tag")
public class PostTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 帖子ID */
    private Long postId;

    /** 标签ID */
    private Long tagId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}

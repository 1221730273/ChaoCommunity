package com.ljc.chaocommunity.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子-文件关联
 */
@Data
@TableName("post_file")
public class PostFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 帖子ID */
    private Long postId;

    /** 文件ID（file_record表主键） */
    private Long fileId;

    /** 文件类型 COVER 封面 / CONTENT 正文图片 */
    private String type;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

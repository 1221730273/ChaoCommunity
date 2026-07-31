package com.ljc.chaocommunity.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审核图片关系表
 */
@Data
@TableName("post_audit_file")
public class PostAuditFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 审核记录ID */
    private Long auditId;

    /** 文件ID（file_record表主键） */
    private Long fileId;

    /** 文件类型 COVER 封面 / CONTENT 正文图片 */
    private String type;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

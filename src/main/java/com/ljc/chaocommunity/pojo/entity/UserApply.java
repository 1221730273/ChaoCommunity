package com.ljc.chaocommunity.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户资料审核记录
 */
@Data
@TableName("user_apply")
public class UserApply {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请用户ID */
    private Long userId;

    /** 审核类型 NICKNAME / AVATAR / PROFILE */
    private String type;

    /** 申请昵称 */
    private String nickname;

    /** 头像文件ID */
    private Long avatarFileId;

    /** 个性签名 */
    private String signature;

    /** 审核状态 0=待审核 1=已通过 2=已驳回 */
    private Integer status;

    /** 驳回原因 */
    private String rejectReason;

    /** 处理管理员ID */
    private Long handlerId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

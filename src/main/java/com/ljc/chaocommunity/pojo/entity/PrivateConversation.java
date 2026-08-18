package com.ljc.chaocommunity.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私信会话实体：负责"谁和谁在聊天"
 */
@Data
@TableName("private_conversation")
public class PrivateConversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发起者（第一条消息的发送者） */
    private Long user1Id;

    /** 接收者 */
    private Long user2Id;

    /** 0等待对方回应 1正常聊天 */
    private Integer status;

    /** 最后一条消息ID */
    private Long lastMessageId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 注意：updateTime 由 MetaObjectHandler 在 updateById 时自动填充，勿用 LambdaUpdateWrapper 更新会话 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

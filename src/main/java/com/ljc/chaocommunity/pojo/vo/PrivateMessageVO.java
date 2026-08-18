package com.ljc.chaocommunity.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私信消息 VO（带发送者快照，WS 推送时接收端可直接渲染）
 */
@Data
public class PrivateMessageVO {

    private Long id;

    private Long conversationId;

    private Long senderId;

    /** 发送者昵称 */
    private String senderNickname;

    /** 发送者头像 */
    private String senderAvatar;

    private String content;

    /** 0未读 1已读 */
    private Integer isRead;

    private LocalDateTime createTime;
}

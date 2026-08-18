package com.ljc.chaocommunity.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私信会话 VO（会话列表展示）
 */
@Data
public class ConversationVO {

    private Long id;

    /** 对方用户ID */
    private Long otherUserId;

    /** 对方昵称 */
    private String otherNickname;

    /** 对方头像 */
    private String otherAvatar;

    /** 0等待对方回应 1正常聊天 */
    private Integer status;

    /** 当前用户是否处于"等待对方回应"：我是发起者且对方尚未回复/关注我（此时我最多只能发一条消息） */
    private Boolean waiting;

    private Long lastMessageId;

    /** 最后一条消息内容 */
    private String lastMessage;

    /** 最后一条消息发送者ID（会话列表预览显示"我:xxx"用） */
    private Long lastSenderId;

    /** 最后一条消息时间 */
    private LocalDateTime lastMessageTime;

    /** 该会话未读数 */
    private Integer unreadCount;
}

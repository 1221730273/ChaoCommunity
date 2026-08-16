package com.ljc.chaocommunity.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知 VO（与 Notification 实体快照字段一一对应，零 join 组装）
 */
@Data
public class NotificationVO {

    private Long id;

    private Long receiverId;

    private Long senderId;

    private String senderNickname;

    private String senderAvatar;

    private Integer type;

    private Long postId;

    private String postTitle;

    private Long commentId;

    private Long parentCommentId;

    private String content;

    private String parentContent;

    private Integer isRead;

    private LocalDateTime createTime;
}

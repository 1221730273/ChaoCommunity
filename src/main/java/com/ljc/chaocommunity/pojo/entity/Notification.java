package com.ljc.chaocommunity.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户消息通知实体
 */
@Data
@TableName("user_notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收用户ID */
    private Long receiverId;

    /** 触发用户ID，系统消息为 null */
    private Long senderId;

    /** 触发人昵称快照 */
    private String senderNickname;

    /** 触发人头像快照 */
    private String senderAvatar;

    /** 通知类型：1回复帖子 2回复评论 3点赞帖子 4点赞评论 5关注 6系统 */
    private Integer type;

    /** 帖子ID */
    private Long postId;

    /** 帖子标题快照 */
    private String postTitle;

    /** 评论ID（回复的新评论 / 被点赞的评论） */
    private Long commentId;

    /** 回复评论时父评论ID */
    private Long parentCommentId;

    /** 内容快照（回复的新评论内容 / 被点赞评论内容 / 系统消息文本） */
    private String content;

    /** 回复评论时，接收人的评论内容快照 */
    private String parentContent;

    /** 0未读 1已读 */
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}

package com.ljc.chaocommunity.pojo.mq;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知 MQ 消息体
 * 由业务侧（点赞/评论/关注）发到 RabbitMQ，NotifyConsumer 消费后入库 + 推送。
 * 携带展示所需快照字段，消费者/推送零查库、且不受原帖子/评论/用户被删影响。
 */
@Data
@NoArgsConstructor
public class NotifyMessage {

    /** 接收人ID */
    private Long receiverId;

    /** 触发人ID（系统消息为 null） */
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
}

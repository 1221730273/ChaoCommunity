package com.ljc.chaocommunity.mq;

import com.ljc.chaocommunity.config.NotifyMqConfig;
import com.ljc.chaocommunity.pojo.entity.Comment;
import com.ljc.chaocommunity.pojo.entity.Post;
import com.ljc.chaocommunity.pojo.entity.User;
import com.ljc.chaocommunity.pojo.enums.NotifyType;
import com.ljc.chaocommunity.pojo.mq.NotifyMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 通知消息生产者
 * 业务侧（点赞/评论/关注）动作后调用对应方法，把通知任务发给 RabbitMQ，
 * 由 NotifyConsumer 异步入库 + Redis 未读数 + WebSocket 推送。
 * 发送时将展示所需快照（昵称/头像/标题/评论内容）组装进消息，消费端零查库。
 */
@Component
public class NotifyProducer {

    /** content 字段最大长度（对应表 VARCHAR(500)） */
    private static final int MAX_CONTENT = 500;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private void send(NotifyMessage msg) {
        rabbitTemplate.convertAndSend(NotifyMqConfig.NOTIFY_EXCHANGE, NotifyMqConfig.NOTIFY_ROUTING_KEY, msg);
    }

    /** 截断超长内容，空串归一为 null */
    private static String cut(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        return s.length() > MAX_CONTENT ? s.substring(0, MAX_CONTENT) : s;
    }

    /** 帖子点赞通知：{receiverId}=帖子作者，sender=点赞者 */
    public void sendLikePost(Long receiverId, User sender, Post post) {
        NotifyMessage msg = new NotifyMessage();
        msg.setReceiverId(receiverId);
        msg.setSenderId(sender.getId());
        msg.setSenderNickname(sender.getNickname());
        msg.setSenderAvatar(sender.getAvatar());
        msg.setType(NotifyType.LIKE_POST.getCode());
        msg.setPostId(post.getId());
        msg.setPostTitle(cut(post.getTitle()));
        send(msg);
    }

    /** 评论点赞通知：{receiverId}=被赞评论作者，sender=点赞者 */
    public void sendLikeComment(Long receiverId, User sender, Comment comment) {
        NotifyMessage msg = new NotifyMessage();
        msg.setReceiverId(receiverId);
        msg.setSenderId(sender.getId());
        msg.setSenderNickname(sender.getNickname());
        msg.setSenderAvatar(sender.getAvatar());
        msg.setType(NotifyType.LIKE_COMMENT.getCode());
        msg.setPostId(comment.getPostId());
        msg.setCommentId(comment.getId());
        msg.setContent(cut(comment.getContent()));
        send(msg);
    }

    /** 回复帖子通知（parentId==0 的一级评论）：{receiverId}=帖子作者 */
    public void sendReplyPost(Long receiverId, User sender, Post post, Long newCommentId, String newContent) {
        NotifyMessage msg = new NotifyMessage();
        msg.setReceiverId(receiverId);
        msg.setSenderId(sender.getId());
        msg.setSenderNickname(sender.getNickname());
        msg.setSenderAvatar(sender.getAvatar());
        msg.setType(NotifyType.REPLY_POST.getCode());
        msg.setPostId(post.getId());
        msg.setPostTitle(cut(post.getTitle()));
        msg.setCommentId(newCommentId);
        msg.setContent(cut(newContent));
        send(msg);
    }

    /** 回复评论通知（parentId!=0）：{receiverId}=父评论作者 */
    public void sendReplyComment(Long receiverId, User sender, Post post, Comment parent, Long newCommentId, String newContent) {
        NotifyMessage msg = new NotifyMessage();
        msg.setReceiverId(receiverId);
        msg.setSenderId(sender.getId());
        msg.setSenderNickname(sender.getNickname());
        msg.setSenderAvatar(sender.getAvatar());
        msg.setType(NotifyType.REPLY_COMMENT.getCode());
        msg.setPostId(post.getId());
        msg.setPostTitle(cut(post.getTitle()));
        msg.setCommentId(newCommentId);
        msg.setParentCommentId(parent.getId());
        msg.setContent(cut(newContent));
        msg.setParentContent(cut(parent.getContent()));
        send(msg);
    }

    /** 关注通知：{receiverId}=被关注者，sender=关注者 */
    public void sendFollow(Long receiverId, User sender) {
        NotifyMessage msg = new NotifyMessage();
        msg.setReceiverId(receiverId);
        msg.setSenderId(sender.getId());
        msg.setSenderNickname(sender.getNickname());
        msg.setSenderAvatar(sender.getAvatar());
        msg.setType(NotifyType.FOLLOW.getCode());
        send(msg);
    }
}

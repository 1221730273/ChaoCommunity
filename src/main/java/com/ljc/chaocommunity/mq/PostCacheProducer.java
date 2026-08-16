package com.ljc.chaocommunity.mq;

import com.ljc.chaocommunity.config.PostEventMqConfig;
import com.ljc.chaocommunity.pojo.enums.PostEventType;
import com.ljc.chaocommunity.pojo.mq.PostEventMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 帖子事件生产者：帖子变化（删除/审核通过/精选切换）后发消息，
 * 由 PostCacheConsumer 异步失效首页精选缓存
 */
@Component
public class PostCacheProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private void send(PostEventMessage msg) {
        rabbitTemplate.convertAndSend(PostEventMqConfig.POST_EVENT_EXCHANGE, PostEventMqConfig.POST_EVENT_ROUTING_KEY, msg);
    }

    /** 帖子被删除（用户/管理员） */
    public void sendPostDeleted(Long postId, boolean isFeatured) {
        PostEventMessage msg = new PostEventMessage();
        msg.setType(PostEventType.POST_DELETED);
        msg.setPostId(postId);
        msg.setIsFeatured(isFeatured);
        send(msg);
    }

    /** 帖子审核通过（内容/封面更新） */
    public void sendAuditApproved(Long postId, boolean isFeatured) {
        PostEventMessage msg = new PostEventMessage();
        msg.setType(PostEventType.AUDIT_APPROVED);
        msg.setPostId(postId);
        msg.setIsFeatured(isFeatured);
        send(msg);
    }

    /** 精选状态切换（设置精选/取消精选） */
    public void sendFeaturedToggled(Long postId, boolean isFeatured) {
        PostEventMessage msg = new PostEventMessage();
        msg.setType(PostEventType.FEATURED_TOGGLED);
        msg.setPostId(postId);
        msg.setIsFeatured(isFeatured);
        send(msg);
    }
}

package com.ljc.chaocommunity.mq;

import com.ljc.chaocommunity.config.RabbitMqConfig;
import com.ljc.chaocommunity.pojo.entity.Post;
import com.ljc.chaocommunity.pojo.entity.User;
import com.ljc.chaocommunity.pojo.mq.EsSyncMessage;
import com.ljc.chaocommunity.pojo.mq.EsSyncType;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * ES 同步消息生产者
 * 业务侧对帖子/用户增删改后，调用对应方法向消息队列发送 ES 同步消息，
 * 由 EsSyncConsumer 异步写入 ES，业务链路不再直接操作 ES
 */
@Component
public class EsSyncProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private void send(EsSyncMessage msg) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.ES_SYNC_EXCHANGE, RabbitMqConfig.ES_SYNC_ROUTING_KEY, msg);
    }

    // ==================== 帖子 ====================

    /** 写入/覆盖帖子（发帖审核通过、内容/封面变更后） */
    public void sendPostIndex(Post post) {
        EsSyncMessage msg = new EsSyncMessage();
        msg.setType(EsSyncType.POST_INDEX);
        msg.setPost(post);
        send(msg);
    }

    /** 删除帖子 */
    public void sendPostDelete(Long postId) {
        EsSyncMessage msg = new EsSyncMessage();
        msg.setType(EsSyncType.POST_DELETE);
        msg.setId(postId);
        send(msg);
    }

    /** 更新帖子状态（隐藏/公开） */
    public void sendPostUpdateStatus(Long postId, Integer status) {
        EsSyncMessage msg = new EsSyncMessage();
        msg.setType(EsSyncType.POST_UPDATE_STATUS);
        msg.setId(postId);
        msg.setValue(status);
        send(msg);
    }

    /** 更新帖子置顶状态 */
    public void sendPostUpdateTop(Long postId, Integer isTop) {
        EsSyncMessage msg = new EsSyncMessage();
        msg.setType(EsSyncType.POST_UPDATE_TOP);
        msg.setId(postId);
        msg.setValue(isTop);
        send(msg);
    }

    /** 更新帖子点赞数（delta 增量，正负皆可） */
    public void sendPostUpdateLikeCount(Long postId, int delta) {
        EsSyncMessage msg = new EsSyncMessage();
        msg.setType(EsSyncType.POST_UPDATE_LIKE_COUNT);
        msg.setId(postId);
        msg.setDelta(delta);
        send(msg);
    }

    /** 更新帖子浏览数（delta 增量，正负皆可） */
    public void sendPostUpdateViewCount(Long postId, int delta) {
        EsSyncMessage msg = new EsSyncMessage();
        msg.setType(EsSyncType.POST_UPDATE_VIEW_COUNT);
        msg.setId(postId);
        msg.setDelta(delta);
        send(msg);
    }

    /** 更新帖子评论数（delta 增量，正负皆可） */
    public void sendPostUpdateCommentCount(Long postId, int delta) {
        EsSyncMessage msg = new EsSyncMessage();
        msg.setType(EsSyncType.POST_UPDATE_COMMENT_COUNT);
        msg.setId(postId);
        msg.setDelta(delta);
        send(msg);
    }

    /** 批量隐藏某用户的全部帖子（封禁用户时） */
    public void sendPostBatchHideByUser(Long userId) {
        EsSyncMessage msg = new EsSyncMessage();
        msg.setType(EsSyncType.POST_BATCH_HIDE_BY_USER);
        msg.setUserId(userId);
        send(msg);
    }

    // ==================== 用户 ====================

    /** 写入/覆盖用户（注册、资料/头像变更后） */
    public void sendUserIndex(User user) {
        EsSyncMessage msg = new EsSyncMessage();
        msg.setType(EsSyncType.USER_INDEX);
        msg.setUser(user);
        send(msg);
    }

    /** 更新用户封禁状态 */
    public void sendUserUpdateStatus(Long userId, Integer status) {
        EsSyncMessage msg = new EsSyncMessage();
        msg.setType(EsSyncType.USER_UPDATE_STATUS);
        msg.setId(userId);
        msg.setValue(status);
        send(msg);
    }
}

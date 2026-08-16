package com.ljc.chaocommunity.mq;

import com.ljc.chaocommunity.config.PostEventMqConfig;
import com.ljc.chaocommunity.pojo.mq.PostEventMessage;
import com.ljc.chaocommunity.service.Impl.PostServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 帖子事件消费者：收到帖子变化消息后，判断是否需要失效首页精选缓存。
 * - 删除 / 审核通过：仅当该帖子为精选（isFeatured=true）才清理缓存
 * - 精选切换：无论结果（设置/取消）都会改变精选池，一律清理缓存
 * 缓存 key 引用自 PostServiceImpl.HOME_FEATURED_CACHE_KEY
 */
@Component
public class PostCacheConsumer {

    private static final Logger log = LoggerFactory.getLogger(PostCacheConsumer.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @RabbitListener(queues = PostEventMqConfig.POST_EVENT_QUEUE)
    public void onPostEvent(PostEventMessage msg) {
        if (msg == null || msg.getType() == null) {
            return;
        }
        boolean needClear;
        switch (msg.getType()) {
            case FEATURED_TOGGLED:
                // 精选池变化，无论设置/取消都刷新
                needClear = true;
                break;
            case POST_DELETED:
            case AUDIT_APPROVED:
                // 非精选帖子的变化不影响精选缓存
                needClear = Boolean.TRUE.equals(msg.getIsFeatured());
                break;
            default:
                return;
        }
        if (needClear) {
            redisTemplate.delete(PostServiceImpl.HOME_FEATURED_CACHE_KEY);
            log.info("清理首页精选缓存: type={} postId={}", msg.getType(), msg.getPostId());
        }
    }
}

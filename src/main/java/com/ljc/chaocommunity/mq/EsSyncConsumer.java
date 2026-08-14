package com.ljc.chaocommunity.mq;

import com.ljc.chaocommunity.config.RabbitMqConfig;
import com.ljc.chaocommunity.pojo.mq.EsSyncMessage;
import com.ljc.chaocommunity.service.PostSearchService;
import com.ljc.chaocommunity.service.UserSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * ES 同步消息消费者
 * 接收业务侧发来的 ES 同步消息，异步写入 Elasticsearch。
 * 复用 PostSearchService / UserSearchService 的既有写入逻辑（搜索、全量同步仍是同步调用）。
 * 写入方法内部已吞掉异常并记录日志，消费端无需额外重试策略。
 */
@Component
public class EsSyncConsumer {

    private static final Logger log = LoggerFactory.getLogger(EsSyncConsumer.class);

    @Autowired
    private PostSearchService postSearchService;

    @Autowired
    private UserSearchService userSearchService;

    @RabbitListener(queues = RabbitMqConfig.ES_SYNC_QUEUE)
    public void onEsSyncMessage(EsSyncMessage message) {
        if (message == null || message.getType() == null) {
            log.warn("收到空的 ES 同步消息，忽略");
            return;
        }
        try {
            switch (message.getType()) {
                case POST_INDEX:
                    postSearchService.index(message.getPost());
                    break;
                case POST_DELETE:
                    postSearchService.delete(message.getId());
                    break;
                case POST_UPDATE_STATUS:
                    postSearchService.updateStatus(message.getId(), message.getValue());
                    break;
                case POST_UPDATE_TOP:
                    postSearchService.updateTop(message.getId(), message.getValue());
                    break;
                case POST_UPDATE_LIKE_COUNT:
                    postSearchService.updateLikeCount(message.getId(), message.getDelta());
                    break;
                case POST_UPDATE_VIEW_COUNT:
                    postSearchService.updateViewCount(message.getId(), message.getDelta());
                    break;
                case POST_UPDATE_COMMENT_COUNT:
                    postSearchService.updateCommentCount(message.getId(), message.getDelta());
                    break;
                case POST_BATCH_HIDE_BY_USER:
                    postSearchService.batchHideByUserId(message.getUserId());
                    break;
                case USER_INDEX:
                    userSearchService.index(message.getUser());
                    break;
                case USER_UPDATE_STATUS:
                    userSearchService.updateStatus(message.getId(), message.getValue());
                    break;
                default:
                    log.warn("未知的 ES 同步消息类型: {}", message.getType());
            }
        } catch (Exception e) {
            log.error("ES 同步消息处理失败: {} - {}", message.getType(), e.getMessage(), e);
        }
    }
}

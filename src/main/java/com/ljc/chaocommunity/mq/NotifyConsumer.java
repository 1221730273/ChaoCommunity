package com.ljc.chaocommunity.mq;

import com.ljc.chaocommunity.config.NotifyMqConfig;
import com.ljc.chaocommunity.pojo.mq.NotifyMessage;
import com.ljc.chaocommunity.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 通知消息消费者
 * 接收业务侧发来的通知消息：先入库（save），再更新未读数 + WebSocket 推送（pushAfterSave）。
 * 两个步骤拆开，入库失败不会污染未读数/推送。
 */
@Component
public class NotifyConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotifyConsumer.class);

    @Autowired
    private NotificationService notificationService;

    @RabbitListener(queues = NotifyMqConfig.NOTIFY_QUEUE)
    public void onNotifyMessage(NotifyMessage message) {
        if (message == null || message.getReceiverId() == null) {
            log.warn("收到无效的通知消息，忽略");
            return;
        }
        try {
            notificationService.save(message);
            notificationService.pushAfterSave(message);
        } catch (Exception e) {
            log.error("通知消息处理失败: type={} receiver={}", message.getType(), message.getReceiverId(), e);
        }
    }
}

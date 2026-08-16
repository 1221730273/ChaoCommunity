package com.ljc.chaocommunity.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 通知 RabbitMQ 配置：声明通知队列/交换机/绑定
 * 消息转换器复用 RabbitMqConfig 的 jsonMessageConverter（JSON + LocalDateTime）
 */
@Configuration
public class NotifyMqConfig {

    /** 通知交换机（direct） */
    public static final String NOTIFY_EXCHANGE = "chaocommunity.notify.exchange";
    /** 通知队列 */
    public static final String NOTIFY_QUEUE = "chaocommunity.notify.queue";
    /** 通知路由键 */
    public static final String NOTIFY_ROUTING_KEY = "notify.push";

    @Bean
    public Queue notifyQueue() {
        return new Queue(NOTIFY_QUEUE, true);
    }

    @Bean
    public DirectExchange notifyExchange() {
        return new DirectExchange(NOTIFY_EXCHANGE, true, false);
    }

    @Bean
    public Binding notifyBinding() {
        return BindingBuilder.bind(notifyQueue()).to(notifyExchange()).with(NOTIFY_ROUTING_KEY);
    }
}

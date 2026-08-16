package com.ljc.chaocommunity.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 帖子事件 RabbitMQ 配置：声明队列/交换机/绑定
 * 消息转换器复用 RabbitMqConfig 的 jsonMessageConverter
 */
@Configuration
public class PostEventMqConfig {

    /** 帖子事件交换机（direct） */
    public static final String POST_EVENT_EXCHANGE = "chaocommunity.post.event.exchange";
    /** 帖子事件队列 */
    public static final String POST_EVENT_QUEUE = "chaocommunity.post.event.queue";
    /** 帖子事件路由键 */
    public static final String POST_EVENT_ROUTING_KEY = "post.event";

    @Bean
    public Queue postEventQueue() {
        return new Queue(POST_EVENT_QUEUE, true);
    }

    @Bean
    public DirectExchange postEventExchange() {
        return new DirectExchange(POST_EVENT_EXCHANGE, true, false);
    }

    @Bean
    public Binding postEventBinding() {
        return BindingBuilder.bind(postEventQueue()).to(postEventExchange()).with(POST_EVENT_ROUTING_KEY);
    }
}

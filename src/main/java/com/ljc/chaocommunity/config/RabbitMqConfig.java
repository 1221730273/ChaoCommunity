package com.ljc.chaocommunity.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置：声明 ES 同步队列/交换机/绑定，并使用 JSON 消息转换器
 * ES 同步链路：业务侧增删改 → EsSyncProducer 发消息 → EsSyncConsumer 异步写 ES
 */
@Configuration
@EnableRabbit
public class RabbitMqConfig {

    /** ES 同步交换机（direct） */
    public static final String ES_SYNC_EXCHANGE = "chaocommunity.es.exchange";
    /** ES 同步队列 */
    public static final String ES_SYNC_QUEUE = "chaocommunity.es.sync.queue";
    /** ES 同步路由键 */
    public static final String ES_SYNC_ROUTING_KEY = "es.sync";

    @Bean
    public Queue esSyncQueue() {
        return new Queue(ES_SYNC_QUEUE, true);
    }

    @Bean
    public DirectExchange esSyncExchange() {
        return new DirectExchange(ES_SYNC_EXCHANGE, true, false);
    }

    @Bean
    public Binding esSyncBinding() {
        return BindingBuilder.bind(esSyncQueue()).to(esSyncExchange()).with(ES_SYNC_ROUTING_KEY);
    }

    /**
     * JSON 消息转换器：RabbitTemplate 与 @RabbitListener 容器工厂都会自动使用。
     * 使用 Spring Boot 自动配置的 ObjectMapper（已注册 JavaTimeModule，支持 LocalDateTime 序列化），
     * 并显式信任应用包，允许按 __TypeId__ 头反序列化 EsSyncMessage。
     */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        ((DefaultJackson2JavaTypeMapper) converter.getJavaTypeMapper()).setTrustedPackages(
                "com.ljc.chaocommunity", "java.util", "java.lang", "java.time");
        return converter;
    }
}

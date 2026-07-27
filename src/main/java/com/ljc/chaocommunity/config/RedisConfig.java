package com.ljc.chaocommunity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 序列化配置
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // 连接工厂（负责与Redis建立TCP连接）
        template.setConnectionFactory(factory);

        // key 序列化：全部用 String，REDIS 里存的 key 就是可读的字符串
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // value 序列化：用 Jackson 转成 JSON 字符串，存进去的是什么类，反序列化就能恢复成什么类
        // GenericJackson2JsonRedisSerializer 会在 JSON 里自动写入 @class 类型信息，反序列化时不需要手动指定 Class
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // 以上配置必须在 setConnectionFactory 之后、afterPropertiesSet 之前
        template.afterPropertiesSet();
        return template;
    }
}

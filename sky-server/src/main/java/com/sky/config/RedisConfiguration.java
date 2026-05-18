package com.sky.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Slf4j
@Configuration
public class RedisConfiguration {

    @Bean
    public LettuceClientConfigurationBuilderCustomizer lettuceCustomizer() {
        return builder -> builder.clientOptions(
                ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build()
        );
    }

    /**
     * 如果不配置这个Bean：
     * 默认使用JdkSerializationRedisSerializer
     * Key会被序列化成类似 \xac\xed\x00\x05t\x00\x04name 这样的二进制格式
     * 在Redis客户端中看到的key会是乱码，难以阅读和调试
     * 配置后的效果：
     * Key以纯字符串形式存储，如 dish_category_1
     * 可读性好，便于在Redis CLI或可视化工具中查看和管理
     * */

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("开始创建RedisTemplate对象...");
        RedisTemplate redisTemplate = new RedisTemplate();
        // 设置Redis连接工厂
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // 设置redis key的序列化器
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        return redisTemplate;
    }
}
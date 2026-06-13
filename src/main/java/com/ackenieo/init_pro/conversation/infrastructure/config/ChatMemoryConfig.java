package com.ackenieo.init_pro.conversation.infrastructure.config;

import com.ackenieo.init_pro.conversation.domain.repository.ChatMemory;
import com.ackenieo.init_pro.conversation.infrastructure.persistence.RedisChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 对话记忆配置
 */
@Configuration
public class ChatMemoryConfig {

    @Value("${live2d-coach.memory.max-messages:20}")
    private int maxMessages;

    @Value("${live2d-coach.memory.ttl-hours:24}")
    private long ttlHours;

    @Value("${live2d-coach.memory.key-prefix:live2d-coach:memory:}")
    private String keyPrefix;

    @Bean
    public ChatMemory chatMemory(StringRedisTemplate redisTemplate) {
        return new RedisChatMemory(
                redisTemplate,
                maxMessages,
                Duration.ofHours(ttlHours),
                keyPrefix
        );
    }
}

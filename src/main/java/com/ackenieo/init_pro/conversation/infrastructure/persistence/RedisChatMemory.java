package com.ackenieo.init_pro.conversation.infrastructure.persistence;

import com.ackenieo.init_pro.conversation.domain.repository.ChatMemory;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于 Redis List 的 ChatMemory 实现
 */
public class RedisChatMemory implements ChatMemory {

    private static final Logger log = LoggerFactory.getLogger(RedisChatMemory.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final int maxMessages;
    private final Duration ttl;
    private final String keyPrefix;

    public RedisChatMemory(StringRedisTemplate redisTemplate,
                           int maxMessages,
                           Duration ttl,
                           String keyPrefix) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.maxMessages = maxMessages;
        this.ttl = ttl;
        this.keyPrefix = keyPrefix;
    }

    private String buildKey(String conversationId) {
        return keyPrefix + conversationId;
    }

    @Override
    public void add(String conversationId, String role, String text) {
        try {
            String key = buildKey(conversationId);
            String json = serialize(role, text);
            redisTemplate.opsForList().rightPush(key, json);

            if (maxMessages > 0) {
                redisTemplate.opsForList().trim(key, -maxMessages, -1);
            }

            if (ttl != null) {
                redisTemplate.expire(key, ttl);
            }
            log.debug("RedisChatMemory 写入: conversationId={}, role={}", conversationId, role);
        } catch (Exception e) {
            log.warn("RedisChatMemory 写入失败: {}", e.getMessage());
        }
    }

    @Override
    public List<Map<String, String>> get(String conversationId) {
        try {
            String key = buildKey(conversationId);
            List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);
            if (jsonList == null || jsonList.isEmpty()) {
                return List.of();
            }
            List<Map<String, String>> result = new ArrayList<>();
            for (String json : jsonList) {
                MessagePayload payload = deserialize(json);
                if (payload != null) {
                    result.add(Map.of("role", payload.role, "text", payload.content));
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("RedisChatMemory 读取失败: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public String getSummary(String conversationId) {
        List<Map<String, String>> messages = get(conversationId);
        if (messages.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, String> msg : messages) {
            sb.append(msg.get("role")).append(": ").append(msg.get("text")).append("\n");
        }
        return sb.toString();
    }

    @Override
    public void clear(String conversationId) {
        try {
            String key = buildKey(conversationId);
            redisTemplate.delete(key);
            log.debug("RedisChatMemory 清空: conversationId={}", conversationId);
        } catch (Exception e) {
            log.warn("RedisChatMemory 清空失败: {}", e.getMessage());
        }
    }

    private String serialize(String role, String content) {
        try {
            return objectMapper.writeValueAsString(new MessagePayload(role, content));
        } catch (JsonProcessingException e) {
            log.warn("序列化失败: {}", e.getMessage());
            return "{}";
        }
    }

    private MessagePayload deserialize(String json) {
        try {
            return objectMapper.readValue(json, MessagePayload.class);
        } catch (Exception e) {
            log.warn("反序列化失败: {}", e.getMessage());
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MessagePayload(String role, String content) {
    }
}

package com.ackenieo.init_pro.conversation.domain.repository;

import com.ackenieo.init_pro.conversation.domain.entity.ChatMessage;

import java.util.List;
import java.util.Optional;

/**
 * 对话消息仓储接口
 */
public interface ChatMessageRepository {
    ChatMessage save(ChatMessage message);
    List<ChatMessage> findBySessionId(String sessionId);
    Optional<ChatMessage> findBySessionIdAndTurnId(String sessionId, String turnId);
}

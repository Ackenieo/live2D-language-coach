package com.ackenieo.init_pro.conversation.domain.repository;

import com.ackenieo.init_pro.conversation.domain.entity.ChatSession;

import java.util.Optional;

/**
 * 对话会话仓储接口
 */
public interface ChatSessionRepository {
    ChatSession save(ChatSession session);
    Optional<ChatSession> findById(String id);
}

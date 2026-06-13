package com.ackenieo.init_pro.conversation.domain.repository;

import com.ackenieo.init_pro.conversation.domain.entity.ChatSession;

import java.util.List;
import java.util.Optional;

/**
 * 对话会话仓储接口
 */
public interface ChatSessionRepository {
    ChatSession save(ChatSession session);
    Optional<ChatSession> findById(String id);
    List<ChatSession> findEndedSessionsByUserId(String userId, int offset, int limit);
    List<ChatSession> findAllEndedSessions(int offset, int limit);
    long countEndedSessionsByUserId(String userId);
}

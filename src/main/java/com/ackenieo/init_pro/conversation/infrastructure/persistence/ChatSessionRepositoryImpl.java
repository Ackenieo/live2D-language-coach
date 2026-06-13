package com.ackenieo.init_pro.conversation.infrastructure.persistence;

import com.ackenieo.init_pro.conversation.domain.entity.ChatSession;
import com.ackenieo.init_pro.conversation.domain.repository.ChatSessionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 对话会话仓储实现
 */
@Repository
public class ChatSessionRepositoryImpl implements ChatSessionRepository {

    private final ChatSessionMapper chatSessionMapper;

    public ChatSessionRepositoryImpl(ChatSessionMapper chatSessionMapper) {
        this.chatSessionMapper = chatSessionMapper;
    }

    @Override
    public ChatSession save(ChatSession session) {
        if (session.getId() == null) {
            chatSessionMapper.insert(session);
        } else {
            chatSessionMapper.updateById(session);
        }
        return session;
    }

    @Override
    public Optional<ChatSession> findById(String id) {
        return Optional.ofNullable(chatSessionMapper.selectById(id));
    }
}

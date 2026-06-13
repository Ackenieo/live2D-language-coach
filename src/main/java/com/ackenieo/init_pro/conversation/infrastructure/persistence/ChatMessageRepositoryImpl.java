package com.ackenieo.init_pro.conversation.infrastructure.persistence;

import com.ackenieo.init_pro.conversation.domain.entity.ChatMessage;
import com.ackenieo.init_pro.conversation.domain.repository.ChatMessageRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ChatMessageRepositoryImpl implements ChatMessageRepository {

    private final ChatMessageMapper chatMessageMapper;

    public ChatMessageRepositoryImpl(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
    }

    @Override
    public ChatMessage save(ChatMessage message) {
        if (message.getId() == null) {
            chatMessageMapper.insert(message);
        } else {
            chatMessageMapper.updateById(message);
        }
        return message;
    }

    @Override
    public List<ChatMessage> findBySessionId(String sessionId) {
        return chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getCreatedAt)
        );
    }

    @Override
    public Optional<ChatMessage> findBySessionIdAndTurnId(String sessionId, String turnId) {
        return Optional.ofNullable(chatMessageMapper.selectOne(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .eq(ChatMessage::getTurnId, turnId)
                        .orderByDesc(ChatMessage::getCreatedAt)
                        .last("LIMIT 1")
        ));
    }
}

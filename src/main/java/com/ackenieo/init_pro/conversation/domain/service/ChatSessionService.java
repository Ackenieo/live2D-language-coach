package com.ackenieo.init_pro.conversation.domain.service;

import com.ackenieo.init_pro.conversation.domain.entity.ChatSession;
import com.ackenieo.init_pro.conversation.domain.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 对话会话应用服务
 * 编排对话生命周期
 */
@Service
public class ChatSessionService {
    private static final Logger log = LoggerFactory.getLogger(ChatSessionService.class);

    private final ChatSessionRepository chatSessionRepository;

    public ChatSessionService(ChatSessionRepository chatSessionRepository) {
        this.chatSessionRepository = chatSessionRepository;
    }

    public ChatSession startSession(String userId, String scene, String difficulty, String accent) {
        ChatSession session = new ChatSession(userId, scene, difficulty, accent);
        ChatSession saved = chatSessionRepository.save(session);
        log.info("创建对话会话: sessionId={}, userId={}, scene={}", saved.getId(), userId, scene);
        return saved;
    }

    public ChatSession startSession(String userId, String scene, String difficulty, String accent, String sessionId) {
        ChatSession session = new ChatSession(userId, scene, difficulty, accent);
        session.setId(sessionId);
        ChatSession saved = chatSessionRepository.save(session);
        log.info("创建对话会话(指定ID): sessionId={}, userId={}, scene={}", saved.getId(), userId, scene);
        return saved;
    }

    public ChatSession updateSessionConfig(String sessionId, String scene, String difficulty, String accent) {
        Optional<ChatSession> opt = chatSessionRepository.findById(sessionId);
        if (opt.isEmpty()) {
            log.warn("更新会话配置失败, 会话不存在: {}", sessionId);
            return null;
        }
        ChatSession session = opt.get();
        session.setScene(scene);
        session.setDifficulty(difficulty);
        session.setAccent(accent);
        session.markUpdated();
        return chatSessionRepository.save(session);
    }

    public ChatSession endSession(String sessionId, SessionAggregate aggregate) {
        Optional<ChatSession> opt = chatSessionRepository.findById(sessionId);
        if (opt.isEmpty()) {
            log.warn("对话会话不存在: {}", sessionId);
            return null;
        }
        ChatSession session = opt.get();
        session.setEndedAt(aggregate.endedAt());
        session.setDurationSeconds(aggregate.durationSeconds());
        session.setMessageCount(aggregate.messageCount());
        session.setOverallScore(aggregate.overallScore());
        session.setAccuracyScore(aggregate.accuracyScore());
        session.setFluencyScore(aggregate.fluencyScore());
        session.setCompletenessScore(aggregate.completenessScore());
        session.setSuggestion(aggregate.suggestion());
        session.setTotalGrade(aggregate.overallScore());
        session.markUpdated();
        return chatSessionRepository.save(session);
    }

    public Optional<ChatSession> findById(String sessionId) {
        return chatSessionRepository.findById(sessionId);
    }

    public List<ChatSession> findEndedSessionsByUserId(String userId, int offset, int limit) {
        return chatSessionRepository.findEndedSessionsByUserId(userId, offset, limit);
    }
}

package com.ackenieo.init_pro.conversation.application.service;

import com.ackenieo.init_pro.conversation.domain.entity.ChatSession;
import com.ackenieo.init_pro.conversation.domain.repository.ChatSessionRepository;
import com.ackenieo.init_pro.conversation.interfaces.dto.ChatHistoryItemResponse;
import com.ackenieo.init_pro.conversation.interfaces.dto.ChatHistoryResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 对话历史应用服务
 */
@Service
public class ChatHistoryAppService {

    private final ChatSessionRepository chatSessionRepository;

    public ChatHistoryAppService(ChatSessionRepository chatSessionRepository) {
        this.chatSessionRepository = chatSessionRepository;
    }

    public ChatHistoryResponse getHistory(String userId, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        int offset = (safePage - 1) * safePageSize;

        List<ChatSession> sessions = chatSessionRepository.findEndedSessionsByUserId(userId, offset, safePageSize);
        long total = chatSessionRepository.countEndedSessionsByUserId(userId);

        List<ChatHistoryItemResponse> records = sessions.stream().map(session -> {
            ChatHistoryItemResponse item = new ChatHistoryItemResponse();
            item.setSessionId(session.getId());
            item.setScene(session.getScene());
            item.setDifficulty(session.getDifficulty());
            item.setOverallGrade(session.getOverallScore() != null && !session.getOverallScore().isBlank()
                    ? session.getOverallScore()
                    : session.getTotalGrade());
            item.setDurationSeconds(session.getDurationSeconds() == null ? 0 : session.getDurationSeconds());
            item.setMessageCount(session.getMessageCount() == null ? 0 : session.getMessageCount());
            item.setCreatedAt(session.getCreatedAt());
            return item;
        }).toList();

        ChatHistoryResponse response = new ChatHistoryResponse();
        response.setRecords(records);
        response.setTotal(total);
        response.setPage(safePage);
        response.setPageSize(safePageSize);
        return response;
    }
}

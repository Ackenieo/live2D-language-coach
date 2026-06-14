package com.ackenieo.init_pro.conversation.application.service;

import com.ackenieo.init_pro.conversation.domain.entity.ChatMessage;
import com.ackenieo.init_pro.conversation.domain.entity.ChatSession;
import com.ackenieo.init_pro.conversation.domain.repository.ChatMessageRepository;
import com.ackenieo.init_pro.conversation.domain.repository.ChatSessionRepository;
import com.ackenieo.init_pro.conversation.interfaces.dto.ChatReportResponse;
import com.ackenieo.init_pro.evaluation.domain.entity.PronunciationResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 会话报告应用服务
 */
@Service
public class ReportAppService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ReportAppService(ChatSessionRepository chatSessionRepository,
                            ChatMessageRepository chatMessageRepository) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    public ChatReportResponse getReport(String userId, String sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));
        if (!userId.equals(session.getUserId())) {
            throw new RuntimeException("会话不存在");
        }

        List<ChatMessage> messages = chatMessageRepository.findBySessionId(sessionId);
        List<ChatMessage> userMessages = messages.stream()
                .filter(message -> "user".equals(message.getRole()))
                .toList();

        Double avgAccuracy = average(userMessages.stream().map(ChatMessage::getPronAccuracy).toList());
        Double avgFluency = average(userMessages.stream().map(ChatMessage::getPronFluency).toList());
        Double avgCompletion = average(userMessages.stream().map(ChatMessage::getPronCompletion).toList());
        Double avgOverall = average(Arrays.asList(avgAccuracy, avgFluency, avgCompletion));

        ChatReportResponse response = new ChatReportResponse();
        response.setSessionId(session.getId());
        response.setScene(session.getScene());
        response.setDifficulty(session.getDifficulty());
        response.setAccent(session.getAccent());
        response.setDurationSeconds(defaultInteger(session.getDurationSeconds()));
        response.setMessageCount(defaultInteger(session.getMessageCount(), messages.size()));
        response.setOverallGrade(defaultGrade(session.getOverallScore(), PronunciationResult.fromScore(avgOverall)));
        response.setAccuracyGrade(defaultGrade(session.getAccuracyScore(), PronunciationResult.fromScore(avgAccuracy)));
        response.setFluencyGrade(defaultGrade(session.getFluencyScore(), PronunciationResult.fromScore(avgFluency)));
        response.setCompletenessGrade(defaultGrade(session.getCompletenessScore(), PronunciationResult.fromScore(avgCompletion)));
        response.setSuggestions(parseSuggestions(session.getSuggestion()));
        response.setCreatedAt(session.getCreatedAt());
        return response;
    }

    private Double average(List<Double> values) {
        List<Double> validValues = values.stream()
                .filter(value -> value != null)
                .toList();
        if (validValues.isEmpty()) {
            return null;
        }
        return validValues.stream().mapToDouble(Double::doubleValue).average().orElse(0D);
    }

    private Integer defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private Integer defaultInteger(Integer value, int fallback) {
        return value == null || value == 0 ? fallback : value;
    }

    private String defaultGrade(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback == null ? "-" : fallback;
    }

    private List<String> parseSuggestions(String suggestion) {
        if (suggestion == null || suggestion.isBlank()) {
            return List.of();
        }
        String normalized = suggestion.replace("\r", "");
        List<String> items = new ArrayList<>();
        for (String line : normalized.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                items.add(trimmed);
            }
        }
        return items;
    }
}

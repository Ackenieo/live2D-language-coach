package com.ackenieo.init_pro.conversation.application.service;

import com.ackenieo.init_pro.conversation.domain.entity.ChatMessage;
import com.ackenieo.init_pro.conversation.domain.entity.ChatSession;
import com.ackenieo.init_pro.conversation.domain.repository.ChatMessageRepository;
import com.ackenieo.init_pro.conversation.domain.service.ChatSessionService;
import com.ackenieo.init_pro.conversation.domain.service.SessionAggregate;
import com.ackenieo.init_pro.evaluation.domain.entity.PronunciationResult;
import com.ackenieo.init_pro.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 会话结束聚合应用服务
 */
@Service
public class SessionFinalizeAppService {

    private final ChatSessionService chatSessionService;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public SessionFinalizeAppService(ChatSessionService chatSessionService,
                                     ChatMessageRepository chatMessageRepository,
                                     UserRepository userRepository) {
        this.chatSessionService = chatSessionService;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }

    public SessionFinalizeResult finalizeSession(String sessionId, List<String> grammarCorrections) {
        ChatSession session = chatSessionService.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));
        List<ChatMessage> messages = chatMessageRepository.findBySessionId(sessionId);
        List<ChatMessage> userMessages = messages.stream()
                .filter(message -> "user".equals(message.getRole()))
                .toList();

        Double avgAccuracy = average(userMessages.stream().map(ChatMessage::getPronAccuracy).toList());
        Double avgFluency = average(userMessages.stream().map(ChatMessage::getPronFluency).toList());
        Double avgCompletion = average(userMessages.stream().map(ChatMessage::getPronCompletion).toList());
        Double avgOverall = average(Arrays.asList(avgAccuracy, avgFluency, avgCompletion));

        LocalDateTime endedAt = LocalDateTime.now();
        int durationSeconds = session.getCreatedAt() == null
                ? 0
                : Math.max((int) Duration.between(session.getCreatedAt(), endedAt).getSeconds(), 0);
        int messageCount = messages.size();

        String suggestion = buildSuggestion(grammarCorrections, avgAccuracy, avgFluency, avgCompletion);
        ChatSession endedSession = chatSessionService.endSession(sessionId, new SessionAggregate(
                endedAt,
                durationSeconds,
                messageCount,
                PronunciationResult.fromScore(avgOverall),
                PronunciationResult.fromScore(avgAccuracy),
                PronunciationResult.fromScore(avgFluency),
                PronunciationResult.fromScore(avgCompletion),
                suggestion
        ));

        updateUserTotalScore(endedSession.getUserId());

        return new SessionFinalizeResult(
                endedSession,
                durationSeconds,
                messageCount,
                endedSession.getOverallScore(),
                endedSession.getAccuracyScore(),
                endedSession.getFluencyScore(),
                endedSession.getCompletenessScore(),
                suggestion
        );
    }

    private void updateUserTotalScore(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            List<ChatSession> sessions = chatSessionService.findEndedSessionsByUserId(userId, 0, 1000);
            List<Double> scores = sessions.stream()
                    .map(ChatSession::getOverallScore)
                    .map(this::gradeToScore)
                    .filter(value -> value != null)
                    .toList();
            if (scores.isEmpty()) {
                user.setTotalScore(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP));
            } else {
                double average = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0D);
                user.setTotalScore(BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP));
            }
            user.markUpdated();
            userRepository.save(user);
        });
    }

    private Double average(List<Double> values) {
        List<Double> validValues = values.stream().filter(value -> value != null).toList();
        if (validValues.isEmpty()) {
            return null;
        }
        return validValues.stream().mapToDouble(Double::doubleValue).average().orElse(0D);
    }

    private String buildSuggestion(List<String> grammarCorrections,
                                   Double avgAccuracy,
                                   Double avgFluency,
                                   Double avgCompletion) {
        List<String> suggestions = new ArrayList<>();
        if (grammarCorrections != null) {
            suggestions.addAll(grammarCorrections.stream().filter(text -> text != null && !text.isBlank()).limit(3).toList());
        }
        if (avgAccuracy != null && avgAccuracy < 70) {
            suggestions.add("注意发音准确度，尤其是容易混淆的音标。");
        }
        if (avgFluency != null && avgFluency < 70) {
            suggestions.add("尝试放慢语速并保持句子连贯。");
        }
        if (avgCompletion != null && avgCompletion < 70) {
            suggestions.add("回答时尽量把句子表达完整。");
        }
        return suggestions.stream().distinct().limit(5).reduce((left, right) -> left + "\n" + right).orElse("");
    }

    private Double gradeToScore(String grade) {
        if (grade == null || grade.isBlank() || "-".equals(grade)) {
            return null;
        }
        return switch (grade) {
            case "S" -> 100D;
            case "A" -> 90D;
            case "B" -> 80D;
            case "C" -> 60D;
            case "D" -> 45D;
            case "E" -> 20D;
            default -> null;
        };
    }

    public record SessionFinalizeResult(
            ChatSession session,
            int durationSeconds,
            int messageCount,
            String overallGrade,
            String accuracyGrade,
            String fluencyGrade,
            String completenessGrade,
            String suggestion
    ) {
    }
}

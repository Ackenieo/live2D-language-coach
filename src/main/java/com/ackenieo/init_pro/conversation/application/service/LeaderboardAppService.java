package com.ackenieo.init_pro.conversation.application.service;

import com.ackenieo.init_pro.conversation.domain.entity.ChatSession;
import com.ackenieo.init_pro.conversation.domain.repository.ChatSessionRepository;
import com.ackenieo.init_pro.conversation.interfaces.dto.LeaderboardItemResponse;
import com.ackenieo.init_pro.conversation.interfaces.dto.LeaderboardResponse;
import com.ackenieo.init_pro.evaluation.domain.model.GradeScale;
import com.ackenieo.init_pro.user.domain.entity.User;
import com.ackenieo.init_pro.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 排行榜应用服务
 */
@Service
public class LeaderboardAppService {

    private final ChatSessionRepository chatSessionRepository;
    private final UserRepository userRepository;

    public LeaderboardAppService(ChatSessionRepository chatSessionRepository,
                                 UserRepository userRepository) {
        this.chatSessionRepository = chatSessionRepository;
        this.userRepository = userRepository;
    }

    public LeaderboardResponse getLeaderboard(String currentUserId, int page, int pageSize) {
        List<LeaderboardItemResponse> ranking = buildRanking();
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        int fromIndex = Math.min((safePage - 1) * safePageSize, ranking.size());
        int toIndex = Math.min(fromIndex + safePageSize, ranking.size());

        LeaderboardResponse response = new LeaderboardResponse();
        response.setRecords(ranking.subList(fromIndex, toIndex));
        response.setMyRank(ranking.stream()
                .filter(item -> item.getUserId().equals(currentUserId))
                .findFirst()
                .orElse(null));
        return response;
    }

    public LeaderboardItemResponse getMyRank(String currentUserId) {
        return buildRanking().stream()
                .filter(item -> item.getUserId().equals(currentUserId))
                .findFirst()
                .orElse(null);
    }

    private List<LeaderboardItemResponse> buildRanking() {
        List<ChatSession> sessions = chatSessionRepository.findAllEndedSessions(0, 10000);
        Map<String, List<ChatSession>> grouped = new HashMap<>();
        for (ChatSession session : sessions) {
            if (session.getUserId() == null) {
                continue;
            }
            grouped.computeIfAbsent(session.getUserId(), key -> new ArrayList<>()).add(session);
        }

        List<LeaderboardItemResponse> ranking = new ArrayList<>();
        for (Map.Entry<String, List<ChatSession>> entry : grouped.entrySet()) {
            String userId = entry.getKey();
            List<ChatSession> userSessions = entry.getValue();
            double avgScore = userSessions.stream()
                    .map(ChatSession::getOverallScore)
                    .map(this::gradeToScore)
                    .filter(value -> value != null)
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0D);
            User user = userRepository.findById(userId).orElse(null);
            LeaderboardItemResponse item = new LeaderboardItemResponse();
            item.setUserId(userId);
            item.setNickname(user == null ? "用户" : user.getNickname());
            item.setAvatarUrl(user == null ? null : user.getAvatarUrl());
            item.setAvgGrade(scoreToGrade(avgScore));
            item.setSessionCount(userSessions.size());
            ranking.add(item);
        }

        ranking.sort(Comparator
                .comparing((LeaderboardItemResponse item) -> gradeToScore(item.getAvgGrade()), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(LeaderboardItemResponse::getSessionCount, Comparator.reverseOrder())
                .thenComparing(LeaderboardItemResponse::getUserId));

        for (int i = 0; i < ranking.size(); i++) {
            ranking.get(i).setRank(i + 1);
        }
        return ranking;
    }

    private Double gradeToScore(String grade) {
        return GradeScale.toRepresentativeScore(grade);
    }

    private String scoreToGrade(double score) {
        return GradeScale.fromScore(score);
    }
}

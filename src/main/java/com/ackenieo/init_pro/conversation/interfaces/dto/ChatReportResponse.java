package com.ackenieo.init_pro.conversation.interfaces.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话报告响应
 */
public class ChatReportResponse {
    private String sessionId;
    private String scene;
    private String difficulty;
    private String accent;
    private Integer durationSeconds;
    private Integer messageCount;
    private String overallGrade;
    private String accuracyGrade;
    private String fluencyGrade;
    private String completenessGrade;
    private List<String> suggestions;
    private LocalDateTime createdAt;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getAccent() {
        return accent;
    }

    public void setAccent(String accent) {
        this.accent = accent;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(Integer messageCount) {
        this.messageCount = messageCount;
    }

    public String getOverallGrade() {
        return overallGrade;
    }

    public void setOverallGrade(String overallGrade) {
        this.overallGrade = overallGrade;
    }

    public String getAccuracyGrade() {
        return accuracyGrade;
    }

    public void setAccuracyGrade(String accuracyGrade) {
        this.accuracyGrade = accuracyGrade;
    }

    public String getFluencyGrade() {
        return fluencyGrade;
    }

    public void setFluencyGrade(String fluencyGrade) {
        this.fluencyGrade = fluencyGrade;
    }

    public String getCompletenessGrade() {
        return completenessGrade;
    }

    public void setCompletenessGrade(String completenessGrade) {
        this.completenessGrade = completenessGrade;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

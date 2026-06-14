package com.ackenieo.init_pro.conversation.domain.entity;

import com.ackenieo.init_pro.shared.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 对话会话实体
 */
@TableName("t_chat_session")
public class ChatSession extends BaseEntity {
    private String userId;
    private String scene;
    private String difficulty;
    private String accent;
    private LocalDateTime endedAt;
    private String totalGrade;
    private String accuracyScore;
    private String fluencyScore;
    private String completenessScore;
    private String overallScore;
    private String suggestion;
    private Integer durationSeconds;
    private Integer messageCount;

    public ChatSession() {
    }

    public ChatSession(String userId, String scene, String difficulty, String accent) {
        this.userId = userId;
        this.scene = scene;
        this.difficulty = difficulty;
        this.accent = accent;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getAccent() { return accent; }
    public void setAccent(String accent) { this.accent = accent; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
    public String getTotalGrade() { return totalGrade; }
    public void setTotalGrade(String totalGrade) { this.totalGrade = totalGrade; }
    public String getAccuracyScore() { return accuracyScore; }
    public void setAccuracyScore(String accuracyScore) { this.accuracyScore = accuracyScore; }
    public String getFluencyScore() { return fluencyScore; }
    public void setFluencyScore(String fluencyScore) { this.fluencyScore = fluencyScore; }
    public String getCompletenessScore() { return completenessScore; }
    public void setCompletenessScore(String completenessScore) { this.completenessScore = completenessScore; }
    public String getOverallScore() { return overallScore; }
    public void setOverallScore(String overallScore) { this.overallScore = overallScore; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
    public Integer getMessageCount() { return messageCount; }
    public void setMessageCount(Integer messageCount) { this.messageCount = messageCount; }
}

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
}

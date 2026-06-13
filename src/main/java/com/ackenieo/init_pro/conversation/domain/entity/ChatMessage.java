package com.ackenieo.init_pro.conversation.domain.entity;

import com.ackenieo.init_pro.shared.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 对话消息实体
 */
@TableName("t_chat_message")
public class ChatMessage extends BaseEntity {
    private String sessionId;
    private String role;
    private String content;
    private String turnId;
    private Double pronAccuracy;
    private Double pronFluency;
    private Double pronCompletion;
    private String accuracyGrade;
    private String fluencyGrade;
    private String completionGrade;

    public ChatMessage() {
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getTurnId() { return turnId; }
    public void setTurnId(String turnId) { this.turnId = turnId; }
    public Double getPronAccuracy() { return pronAccuracy; }
    public void setPronAccuracy(Double pronAccuracy) { this.pronAccuracy = pronAccuracy; }
    public Double getPronFluency() { return pronFluency; }
    public void setPronFluency(Double pronFluency) { this.pronFluency = pronFluency; }
    public Double getPronCompletion() { return pronCompletion; }
    public void setPronCompletion(Double pronCompletion) { this.pronCompletion = pronCompletion; }
    public String getAccuracyGrade() { return accuracyGrade; }
    public void setAccuracyGrade(String accuracyGrade) { this.accuracyGrade = accuracyGrade; }
    public String getFluencyGrade() { return fluencyGrade; }
    public void setFluencyGrade(String fluencyGrade) { this.fluencyGrade = fluencyGrade; }
    public String getCompletionGrade() { return completionGrade; }
    public void setCompletionGrade(String completionGrade) { this.completionGrade = completionGrade; }
}

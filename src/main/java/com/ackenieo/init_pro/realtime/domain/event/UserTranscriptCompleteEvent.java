package com.ackenieo.init_pro.realtime.domain.event;

import com.ackenieo.init_pro.shared.domain.DomainEvent;

/**
 * 用户转写完成事件
 * 百炼完成用户语音转写时发布
 */
public class UserTranscriptCompleteEvent extends DomainEvent {
    private final String sessionId;
    private final String turnId;
    private final String text;

    public UserTranscriptCompleteEvent(String sessionId, String turnId, String text) {
        super(sessionId);
        this.sessionId = sessionId;
        this.turnId = turnId;
        this.text = text;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getTurnId() {
        return turnId;
    }

    public String getText() {
        return text;
    }
}

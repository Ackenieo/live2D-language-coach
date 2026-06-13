package com.ackenieo.init_pro.evaluation.domain.event;

import com.ackenieo.init_pro.shared.domain.DomainEvent;

/**
 * 语法纠正完成事件
 */
public class GrammarCorrectedEvent extends DomainEvent {
    private final String sessionId;
    private final String turnId;
    private final String correctedText;

    public GrammarCorrectedEvent(String sessionId, String turnId, String correctedText) {
        super(turnId);
        this.sessionId = sessionId;
        this.turnId = turnId;
        this.correctedText = correctedText;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getTurnId() {
        return turnId;
    }

    public String getCorrectedText() {
        return correctedText;
    }
}

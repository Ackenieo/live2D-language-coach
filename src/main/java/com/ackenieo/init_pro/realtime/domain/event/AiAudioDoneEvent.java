package com.ackenieo.init_pro.realtime.domain.event;

import com.ackenieo.init_pro.shared.domain.DomainEvent;

/**
 * AI audio stream finished for one realtime response.
 */
public class AiAudioDoneEvent extends DomainEvent {
    private final String sessionId;
    private final String responseId;
    private final String itemId;

    public AiAudioDoneEvent(String sessionId, String responseId, String itemId) {
        super(sessionId);
        this.sessionId = sessionId;
        this.responseId = responseId == null ? "" : responseId;
        this.itemId = itemId == null ? "" : itemId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getResponseId() {
        return responseId;
    }

    public String getItemId() {
        return itemId;
    }
}

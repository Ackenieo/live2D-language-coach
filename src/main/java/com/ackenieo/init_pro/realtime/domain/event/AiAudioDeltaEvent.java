package com.ackenieo.init_pro.realtime.domain.event;

import com.ackenieo.init_pro.shared.domain.DomainEvent;

/**
 * AI音频增量事件
 * 百炼返回音频流片段时发布
 */
public class AiAudioDeltaEvent extends DomainEvent {
    private final String sessionId;
    private final byte[] audioData;

    public AiAudioDeltaEvent(String sessionId, byte[] audioData) {
        super(sessionId);
        this.sessionId = sessionId;
        this.audioData = audioData;
    }

    public String getSessionId() {
        return sessionId;
    }

    public byte[] getAudioData() {
        return audioData;
    }
}

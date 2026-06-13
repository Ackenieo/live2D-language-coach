package com.ackenieo.init_pro.realtime.domain.event;

import com.ackenieo.init_pro.shared.domain.DomainEvent;

/**
 * AI字幕完成事件
 * 百炼返回完整转写文本时发布
 */
public class AiSubtitleCompleteEvent extends DomainEvent {
    private final String sessionId;
    private final String text;

    public AiSubtitleCompleteEvent(String sessionId, String text) {
        super(sessionId);
        this.sessionId = sessionId;
        this.text = text;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getText() {
        return text;
    }
}

package com.ackenieo.init_pro.realtime.domain.event;

import com.ackenieo.init_pro.shared.domain.DomainEvent;

/**
 * AI字幕完成事件
 * 百炼返回完整转写文本时发布
 */
public class AiSubtitleCompleteEvent extends DomainEvent {
    private final String sessionId;
    private final String text;
    private final String responseId;
    private final String itemId;

    public AiSubtitleCompleteEvent(String sessionId, String text) {
        this(sessionId, text, "", "");
    }

    public AiSubtitleCompleteEvent(String sessionId, String text, String responseId, String itemId) {
        super(sessionId);
        this.sessionId = sessionId;
        this.text = text;
        this.responseId = responseId == null ? "" : responseId;
        this.itemId = itemId == null ? "" : itemId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getText() {
        return text;
    }

    public String getResponseId() {
        return responseId;
    }

    public String getItemId() {
        return itemId;
    }
}

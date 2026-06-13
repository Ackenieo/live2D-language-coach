package com.ackenieo.init_pro.evaluation.domain.event;

import com.ackenieo.init_pro.evaluation.domain.entity.PronunciationResult;
import com.ackenieo.init_pro.shared.domain.DomainEvent;

/**
 * 发音评测完成事件
 */
public class PronunciationEvaluatedEvent extends DomainEvent {
    private final PronunciationResult result;

    public PronunciationEvaluatedEvent(PronunciationResult result) {
        super(result.turnId());
        this.result = result;
    }

    public PronunciationResult getResult() {
        return result;
    }
}

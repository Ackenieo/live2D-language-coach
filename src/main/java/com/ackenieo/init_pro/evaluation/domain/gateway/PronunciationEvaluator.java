package com.ackenieo.init_pro.evaluation.domain.gateway;

import com.ackenieo.init_pro.evaluation.domain.entity.PronunciationResult;

import java.util.concurrent.CompletableFuture;

/**
 * 发音评测接口
 */
public interface PronunciationEvaluator {
    CompletableFuture<PronunciationResult> evaluate(String turnId, String sessionId, byte[] audioData, String refText);
    boolean isEnabled();
}

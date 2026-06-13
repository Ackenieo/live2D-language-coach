package com.ackenieo.init_pro.evaluation.domain.service;

import com.ackenieo.init_pro.evaluation.domain.entity.PronunciationResult;
import com.ackenieo.init_pro.evaluation.domain.event.GrammarCorrectedEvent;
import com.ackenieo.init_pro.evaluation.domain.event.PronunciationEvaluatedEvent;
import com.ackenieo.init_pro.evaluation.domain.gateway.GrammarCorrector;
import com.ackenieo.init_pro.evaluation.domain.gateway.PronunciationEvaluator;
import com.ackenieo.init_pro.shared.domain.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 评分编排服务
 * 编排 PronunciationEvaluator + GrammarCorrector
 * 通过 DomainEventPublisher 发布领域事件，不再依赖 EvaluationResultPublisher
 */
@Service
public class EvaluationService {
    private static final Logger log = LoggerFactory.getLogger(EvaluationService.class);

    private final PronunciationEvaluator pronunciationEvaluator;
    private final GrammarCorrector grammarCorrector;
    private final DomainEventPublisher eventPublisher;

    public EvaluationService(PronunciationEvaluator pronunciationEvaluator,
                             GrammarCorrector grammarCorrector,
                             DomainEventPublisher eventPublisher) {
        this.pronunciationEvaluator = pronunciationEvaluator;
        this.grammarCorrector = grammarCorrector;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 评估一轮对话：发音评测 + 语法纠错
     */
    public void evaluateTurn(String turnId, String sessionId,
                             AudioTurnBufferService audioTurnBufferService, String refText) {
        evaluatePronunciation(turnId, sessionId, audioTurnBufferService, refText);
        evaluateGrammar(turnId, sessionId, refText);
    }

    private void evaluatePronunciation(String turnId, String sessionId,
                                       AudioTurnBufferService audioTurnBufferService, String refText) {
        if (pronunciationEvaluator == null || !pronunciationEvaluator.isEnabled() || turnId == null || turnId.isBlank()) {
            return;
        }

        byte[] audioData = audioTurnBufferService.removeAudio(turnId);
        if (audioData.length == 0) {
            log.warn("未找到可用于评分的音频, turnId={}", turnId);
            return;
        }

        pronunciationEvaluator.evaluate(turnId, sessionId, audioData, refText)
                .thenAccept(result -> {
                    if (result != null) {
                        eventPublisher.publish(new PronunciationEvaluatedEvent(result));
                    }
                })
                .exceptionally(ex -> {
                    log.error("异步发音测评失败, turnId={}", turnId, ex);
                    return null;
                });
    }

    private void evaluateGrammar(String turnId, String sessionId, String refText) {
        if (grammarCorrector == null || !grammarCorrector.isEnabled()) {
            return;
        }

        CompletableFuture.supplyAsync(() -> grammarCorrector.correct(refText))
                .thenAccept(corrected -> {
                    if (corrected != null && !corrected.trim().isEmpty() && !"无".equals(corrected.trim())) {
                        eventPublisher.publish(new GrammarCorrectedEvent(sessionId, turnId, corrected));
                    }
                })
                .exceptionally(ex -> {
                    log.error("异步语法纠正失败, turnId={}", turnId, ex);
                    return null;
                });
    }
}

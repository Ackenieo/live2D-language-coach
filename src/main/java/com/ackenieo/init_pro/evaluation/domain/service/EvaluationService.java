package com.ackenieo.init_pro.evaluation.domain.service;

import com.ackenieo.init_pro.evaluation.domain.entity.PronunciationResult;
import com.ackenieo.init_pro.evaluation.domain.gateway.EvaluationResultPublisher;
import com.ackenieo.init_pro.evaluation.domain.gateway.GrammarCorrector;
import com.ackenieo.init_pro.evaluation.domain.gateway.PronunciationEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 评分编排服务
 * 编排 PronunciationEvaluator + GrammarCorrector
 * 通过 EvaluationResultPublisher 发布结果，不再直接依赖 WebSocketSession
 */
@Service
public class EvaluationService {
    private static final Logger log = LoggerFactory.getLogger(EvaluationService.class);

    private final PronunciationEvaluator pronunciationEvaluator;
    private final GrammarCorrector grammarCorrector;

    public EvaluationService(PronunciationEvaluator pronunciationEvaluator,
                             GrammarCorrector grammarCorrector) {
        this.pronunciationEvaluator = pronunciationEvaluator;
        this.grammarCorrector = grammarCorrector;
    }

    /**
     * 评估一轮对话：发音评测 + 语法纠错
     */
    public void evaluateTurn(String turnId, String sessionId,
                             AudioTurnBufferService audioTurnBufferService,
                             EvaluationResultPublisher publisher, String refText) {
        // 发音评测
        if (pronunciationEvaluator != null && pronunciationEvaluator.isEnabled() && turnId != null && !turnId.isBlank()) {
            byte[] audioData = audioTurnBufferService.removeAudio(turnId);
            if (audioData.length > 0) {
                pronunciationEvaluator.evaluate(turnId, sessionId, audioData, refText)
                        .thenAccept(result -> {
                            if (result != null) {
                                publisher.publishPronunciationResult(result);
                            }
                        })
                        .exceptionally(ex -> {
                            log.error("异步发音测评失败, turnId={}", turnId, ex);
                            return null;
                        });
            } else {
                log.warn("未找到可用于评分的音频, turnId={}", turnId);
            }
        }

        // 语法纠错
        if (grammarCorrector != null && grammarCorrector.isEnabled()) {
            CompletableFuture.supplyAsync(() -> grammarCorrector.correct(refText))
                    .thenAccept(corrected -> {
                        if (corrected != null && !corrected.trim().isEmpty() && !"\u65e0".equals(corrected.trim())) {
                            publisher.publishGrammarCorrection(turnId, corrected);
                        }
                    })
                    .exceptionally(ex -> {
                        log.error("异步语法纠正失败, turnId={}", turnId, ex);
                        return null;
                    });
        }
    }
}

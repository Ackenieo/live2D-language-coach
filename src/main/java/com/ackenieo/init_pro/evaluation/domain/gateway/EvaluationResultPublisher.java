package com.ackenieo.init_pro.evaluation.domain.gateway;

import com.ackenieo.init_pro.evaluation.domain.entity.PronunciationResult;

/**
 * 评估结果发布器接口
 * 解耦领域服务与传输层（WebSocket）的依赖
 */
public interface EvaluationResultPublisher {
    /**
     * 发布发音评测结果
     */
    void publishPronunciationResult(PronunciationResult result);

    /**
     * 发布语法纠错结果
     */
    void publishGrammarCorrection(String turnId, String correctedText);
}

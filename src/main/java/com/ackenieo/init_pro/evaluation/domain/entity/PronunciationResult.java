package com.ackenieo.init_pro.evaluation.domain.entity;

import com.ackenieo.init_pro.evaluation.domain.model.GradeScale;

/**
 * 发音测评结果值对象
 * Grade scale: S(85+) / A(60+) / B(45+) / C(30+) / D(15+) / E(<15)
 */
public record PronunciationResult(
        String turnId,
        String sessionId,
        String text,
        Double suggestedScore,
        Double pronAccuracy,
        Double pronFluency,
        Double pronCompletion,
        String suggestedGrade,
        String accuracyGrade,
        String fluencyGrade,
        String completionGrade
) {
    /**
     * 根据分数转换为字母等级
     */
    public static String fromScore(Double score) {
        return GradeScale.fromScore(score);
    }
}

package com.ackenieo.init_pro.evaluation.domain.entity;

import com.ackenieo.init_pro.shared.domain.BaseValueObject;

/**
 * 发音测评结果值对象
 * 等级标准：S(95+) / A(85+) / B(70+) / C(55+) / D(40+) / E(<40)
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
        if (score == null) {
            return "-";
        }
        if (score >= 95) return "S";
        if (score >= 85) return "A";
        if (score >= 70) return "B";
        if (score >= 55) return "C";
        if (score >= 40) return "D";
        return "E";
    }
}

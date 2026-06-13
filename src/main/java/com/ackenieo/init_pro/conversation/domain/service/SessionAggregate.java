package com.ackenieo.init_pro.conversation.domain.service;

import java.time.LocalDateTime;

/**
 * 会话结束聚合结果
 */
public record SessionAggregate(
        LocalDateTime endedAt,
        Integer durationSeconds,
        Integer messageCount,
        String overallScore,
        String accuracyScore,
        String fluencyScore,
        String completenessScore,
        String suggestion
) {
}

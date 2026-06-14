package com.ackenieo.init_pro.shared.infrastructure.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SentinelResourceNameCleanerTest {

    private final SentinelResourceNameCleaner cleaner = new SentinelResourceNameCleaner();

    @Test
    void normalizesChatSessionResources() {
        assertThat(cleaner.clean("/api/chat/sessions/session-1/history"))
                .isEqualTo("/api/chat/sessions/{sessionId}/history");
        assertThat(cleaner.clean("/api/chat/sessions/session-2/summary"))
                .isEqualTo("/api/chat/sessions/{sessionId}/summary");
    }

    @Test
    void normalizesChatReportResourceAndKeepsMethodPrefix() {
        assertThat(cleaner.clean("GET:/api/chat/report/session-1?foo=bar"))
                .isEqualTo("GET:/api/chat/report/{sessionId}");
    }

    @Test
    void keepsStableResourcesUnchanged() {
        assertThat(cleaner.clean("/api/leaderboard/my-rank"))
                .isEqualTo("/api/leaderboard/my-rank");
    }
}

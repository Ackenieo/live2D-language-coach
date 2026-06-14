package com.ackenieo.init_pro.shared.infrastructure.ratelimit;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketRateLimitInterceptorTest {

    @AfterEach
    void resetRules() {
        FlowRuleManager.loadRules(List.of());
    }

    @Test
    void rejectsHandshakeWhenSentinelBlocksResource() {
        FlowRuleManager.loadRules(List.of(new FlowRule(WebSocketRateLimitInterceptor.RESOURCE_NAME)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(0)));
        WebSocketRateLimitInterceptor interceptor = new WebSocketRateLimitInterceptor();
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws/bailian");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        boolean allowed = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse),
                null,
                new HashMap<>()
        );

        assertThat(allowed).isFalse();
        assertThat(servletResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }
}

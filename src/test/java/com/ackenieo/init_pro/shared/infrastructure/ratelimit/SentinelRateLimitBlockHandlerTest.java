package com.ackenieo.init_pro.shared.infrastructure.ratelimit;

import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SentinelRateLimitBlockHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void writesTooManyRequestsApiResponse() throws Exception {
        SentinelRateLimitBlockHandler handler = new SentinelRateLimitBlockHandler(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/chat/history");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, "GET:/api/chat/history", new FlowException("blocked"));

        JsonNode body = objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));
        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(MediaType.parseMediaType(response.getContentType()).isCompatibleWith(MediaType.APPLICATION_JSON))
                .isTrue();
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("data").isNull()).isTrue();
        assertThat(body.get("message").asText()).isEqualTo(SentinelRateLimitBlockHandler.RATE_LIMIT_MESSAGE);
    }
}

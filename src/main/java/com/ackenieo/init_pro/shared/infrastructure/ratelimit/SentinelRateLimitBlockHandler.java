package com.ackenieo.init_pro.shared.infrastructure.ratelimit;

import com.ackenieo.init_pro.shared.infrastructure.ApiResponse;
import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

public class SentinelRateLimitBlockHandler implements BlockExceptionHandler {
    public static final String RATE_LIMIT_MESSAGE = "请求过于频繁，请稍后再试";

    private final ObjectMapper objectMapper;

    public SentinelRateLimitBlockHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       String resourceName, BlockException e) throws Exception {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.failure(RATE_LIMIT_MESSAGE)));
    }
}

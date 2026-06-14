package com.ackenieo.init_pro.shared.infrastructure.ratelimit;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class WebSocketRateLimitInterceptor implements HandshakeInterceptor {
    public static final String RESOURCE_NAME = "WS:/ws/bailian";

    private static final Logger log = LoggerFactory.getLogger(WebSocketRateLimitInterceptor.class);

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try (Entry ignored = SphU.entry(RESOURCE_NAME, EntryType.IN)) {
            return true;
        } catch (BlockException e) {
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            log.warn("WebSocket handshake rate limited, resource={}", RESOURCE_NAME);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}

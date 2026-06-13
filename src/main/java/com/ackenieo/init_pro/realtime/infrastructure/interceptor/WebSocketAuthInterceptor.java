package com.ackenieo.init_pro.realtime.infrastructure.interceptor;

import com.ackenieo.init_pro.user.infrastructure.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 鉴权拦截器
 * 握手时从 ?token=xxx 做 JWT 验证
 */
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {
    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtTokenProvider jwtTokenProvider;

    public WebSocketAuthInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        String query = request.getURI().getQuery();
        if (query == null || !query.contains("token=")) {
            log.warn("WebSocket握手缺少token参数");
            return false;
        }

        String token = extractToken(query);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            log.warn("WebSocket握手token验证失败");
            return false;
        }

        String userId = jwtTokenProvider.getUserIdFromToken(token);
        attributes.put("userId", userId);
        log.info("WebSocket握手成功, userId={}", userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private String extractToken(String query) {
        for (String param : query.split("&")) {
            if (param.startsWith("token=")) {
                return param.substring(6);
            }
        }
        return null;
    }
}

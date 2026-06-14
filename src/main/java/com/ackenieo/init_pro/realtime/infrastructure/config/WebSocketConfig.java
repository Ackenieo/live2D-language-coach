package com.ackenieo.init_pro.realtime.infrastructure.config;

import com.ackenieo.init_pro.realtime.interfaces.ws.ChatWebSocketHandler;
import com.ackenieo.init_pro.realtime.infrastructure.interceptor.WebSocketAuthInterceptor;
import com.ackenieo.init_pro.shared.infrastructure.ratelimit.WebSocketRateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final WebSocketRateLimitInterceptor webSocketRateLimitInterceptor;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler,
                           WebSocketRateLimitInterceptor webSocketRateLimitInterceptor,
                           WebSocketAuthInterceptor webSocketAuthInterceptor) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.webSocketRateLimitInterceptor = webSocketRateLimitInterceptor;
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/bailian")
                .addInterceptors(webSocketRateLimitInterceptor, webSocketAuthInterceptor)
                .setAllowedOrigins("*");
    }
}

package com.ackenieo.init_pro.realtime.interfaces.ws;

import com.ackenieo.init_pro.conversation.domain.service.ConversationMemoryService;
import com.ackenieo.init_pro.conversation.domain.service.PromptTemplateService;
import com.ackenieo.init_pro.realtime.domain.gateway.RealtimeChatClient;
import com.ackenieo.init_pro.realtime.domain.gateway.RealtimeChatClientFactory;
import com.ackenieo.init_pro.realtime.infrastructure.config.BailianConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对话 WebSocket Handler
 * 通过 RealtimeChatClientFactory 创建客户端，不再直接依赖 BailianRealtimeClient
 */
@Component
public class ChatWebSocketHandler extends AbstractWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, RealtimeChatClient> clientMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> visionEnabledMap = new ConcurrentHashMap<>();
    private final ConversationMemoryService memoryService;
    private final RealtimeChatClientFactory clientFactory;
    private final PromptTemplateService promptTemplateService;

    public ChatWebSocketHandler(ConversationMemoryService memoryService,
                                RealtimeChatClientFactory clientFactory,
                                PromptTemplateService promptTemplateService) {
        this.memoryService = memoryService;
        this.clientFactory = clientFactory;
        this.promptTemplateService = promptTemplateService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = UUID.randomUUID().toString();
        session.getAttributes().put("sessionId", sessionId);
        log.info("前端WebSocket连接已建立, sessionId={}", sessionId);

        RealtimeChatClient client = clientFactory.create(session, sessionId);
        clientMap.put(sessionId, client);
        client.connect();
    }

    private RealtimeChatClient getOrCreateClient(WebSocketSession session) throws Exception {
        String sessionId = (String) session.getAttributes().get("sessionId");
        RealtimeChatClient client = clientMap.get(sessionId);
        if (client == null || !client.isConnected()) {
            // WebSocketClient 不可复用，关闭旧实例并创建新的
            if (client != null) {
                try { client.close(); } catch (Exception ignored) {}
            }
            client = clientFactory.create(session, sessionId);
            clientMap.put(sessionId, client);
            client.connect();
        }
        return client;
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        String sessionId = (String) session.getAttributes().get("sessionId");
        RealtimeChatClient client = clientMap.get(sessionId);

        if (client != null && client.isConnected()) {
            ByteBuffer buffer = message.getPayload();
            byte[] audioData = new byte[buffer.remaining()];
            buffer.get(audioData);
            client.sendAudio(audioData);
            log.debug("音频数据到达, sessionId={}, bytes={}", sessionId, audioData.length);
        } else {
            log.warn("音频数据丢弃, sessionId={}, client={}, connected={}", sessionId, client != null, client != null && client.isConnected());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = (String) session.getAttributes().get("sessionId");
        RealtimeChatClient client = clientMap.get(sessionId);
        String payload = message.getPayload();

        try {
            JsonNode json = objectMapper.readTree(payload);
            String type = json.has("type") ? json.get("type").asText() : "";

            switch (type) {
                case "text" -> {
                    if (client != null && client.isConnected()) {
                        client.sendText(json.has("text") ? json.get("text").asText() : "");
                    }
                }
                case "screenshot" -> {
                    if (client != null && client.isConnected()) {
                        if (!memoryService.hasVisualKeywordInLatestUserText(sessionId)) {
                            log.info("图片脱敏拦截: sessionId={}, 最新用户文本无视觉关键词", sessionId);
                            break;
                        }
                        String image = json.has("image") ? json.get("image").asText() : "";
                        String prompt = json.has("prompt") ? json.get("prompt").asText() : "\u8bf7\u7b80\u6d01\u63cf\u8ff0\u8fd9\u5f20\u56fe\u7247\u7684\u5185\u5bb9";
                        if (!image.isEmpty()) {
                            int commaIdx = image.indexOf(',');
                            String pureBase64 = commaIdx >= 0 ? image.substring(commaIdx + 1) : image;
                            client.sendImage(pureBase64, prompt);
                        }
                    }
                }
                case "config" -> {
                    // 处理 vision 字段
                    if (json.has("vision")) {
                        String vision = json.get("vision").asText();
                        boolean hasVision = "on".equalsIgnoreCase(vision);
                        visionEnabledMap.put(sessionId, hasVision);
                        log.info("视觉状态变更, sessionId={}, vision={}", sessionId, vision);
                    }

                    String instructions;
                    if (json.has("lang") || json.has("role")) {
                        String lang = json.has("lang") ? json.get("lang").asText() : "en";
                        String role = json.has("role") ? json.get("role").asText() : "English Coach";
                        boolean hasVision = visionEnabledMap.getOrDefault(sessionId, false);
                        instructions = buildInstructions(lang, role, hasVision);
                        log.info("收到配置更新(前端格式), sessionId={}, lang={}, role={}, vision={}", sessionId, lang, role, hasVision);
                    } else {
                        String scene = json.has("scene") ? json.get("scene").asText() : "default";
                        String difficulty = json.has("difficulty") ? json.get("difficulty").asText() : "medium";
                        String accent = json.has("accent") ? json.get("accent").asText() : "us";
                        instructions = promptTemplateService.getSystemPrompt(scene, Map.of(
                                "difficulty", difficulty,
                                "accent", accent
                        ));
                        log.info("收到配置更新(扩展格式), sessionId={}, scene={}, difficulty={}, accent={}", sessionId, scene, difficulty, accent);
                    }
                    RealtimeChatClient ensuredClient = getOrCreateClient(session);
                    ensuredClient.setInstructions(instructions);
                }
                case "start" -> {
                    RealtimeChatClient ensuredClient = getOrCreateClient(session);
                    log.info("开始通话, connected={}", ensuredClient.isConnected());
                }
                case "finish" -> {
                    if (client != null) client.close();
                }
            }
        } catch (Exception e) {
            log.warn("解析消息失败", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = (String) session.getAttributes().get("sessionId");
        RealtimeChatClient client = clientMap.remove(sessionId);
        visionEnabledMap.remove(sessionId);
        if (client != null) {
            client.clearCurrentTurn();
            client.close();
        }
        log.info("前端WebSocket连接已关闭, sessionId={}", sessionId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误", exception);
        afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
    }

    /**
     * 根据语言、角色和视觉状态构建系统指令
     */
    static String buildInstructions(String lang, String role, boolean hasVision) {
        if ("zh".equalsIgnoreCase(lang)) {
            if (hasVision) {
                return "\u4f60\u662f\u4e00\u540d" + role + "\uff0c\u56fe\u7247\u4fe1\u606f\u662f\u4f60\u773c\u955c\u6240\u770b\u5230\u7684\u5185\u5bb9\u3002\u8bf7\u7b80\u6d01\u56de\u7b54\u5e76\u9002\u65f6\u7ea0\u6b63\u7528\u6237\u7684\u82f1\u8bed\u3002";
            }
            return "\u4f60\u662f\u4e00\u540d" + role + "\u3002\u8bf7\u7b80\u6d01\u56de\u7b54\u5e76\u9002\u65f6\u7ea0\u6b63\u7528\u6237\u7684\u82f1\u8bed\u3002";
        }
        if (hasVision) {
            return "You are " + role + ", the screenshot is what you see with your eyes. Answer concisely and correct the user's English when appropriate.";
        }
        return "You are " + role + ". Answer concisely and correct the user's English when appropriate.";
    }
}

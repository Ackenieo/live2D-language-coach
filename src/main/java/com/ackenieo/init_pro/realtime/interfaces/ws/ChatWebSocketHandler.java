package com.ackenieo.init_pro.realtime.interfaces.ws;

import com.ackenieo.init_pro.conversation.domain.entity.ChatSession;
import com.ackenieo.init_pro.conversation.domain.service.ChatSessionService;
import com.ackenieo.init_pro.conversation.domain.service.ConversationMemoryService;
import com.ackenieo.init_pro.conversation.domain.service.PromptTemplateService;
import com.ackenieo.init_pro.evaluation.domain.entity.PronunciationResult;
import com.ackenieo.init_pro.evaluation.domain.event.GrammarCorrectedEvent;
import com.ackenieo.init_pro.evaluation.domain.event.PronunciationEvaluatedEvent;
import com.ackenieo.init_pro.realtime.domain.event.AiAudioDeltaEvent;
import com.ackenieo.init_pro.realtime.domain.event.AiSubtitleCompleteEvent;
import com.ackenieo.init_pro.realtime.domain.event.AiSubtitleDeltaEvent;
import com.ackenieo.init_pro.realtime.domain.event.UserTranscriptCompleteEvent;
import com.ackenieo.init_pro.realtime.domain.gateway.RealtimeChatClient;
import com.ackenieo.init_pro.realtime.domain.gateway.RealtimeChatClientFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对话 WebSocket Handler
 * 监听领域事件，转发到前端 WebSocket
 */
@Component
public class ChatWebSocketHandler extends AbstractWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** sessionId → RealtimeChatClient */
    private final Map<String, RealtimeChatClient> clientMap = new ConcurrentHashMap<>();
    /** sessionId → 前端 WebSocketSession */
    private final Map<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> visionEnabledMap = new ConcurrentHashMap<>();
    private final ConversationMemoryService memoryService;
    private final RealtimeChatClientFactory clientFactory;
    private final PromptTemplateService promptTemplateService;
    private final ChatSessionService chatSessionService;

    public ChatWebSocketHandler(ConversationMemoryService memoryService,
                                RealtimeChatClientFactory clientFactory,
                                PromptTemplateService promptTemplateService,
                                ChatSessionService chatSessionService) {
        this.memoryService = memoryService;
        this.clientFactory = clientFactory;
        this.promptTemplateService = promptTemplateService;
        this.chatSessionService = chatSessionService;
    }

    // ===== 领域事件监听 → 转发前端 =====

    @EventListener
    public void onAiAudioDelta(AiAudioDeltaEvent event) {
        WebSocketSession session = sessionMap.get(event.getSessionId());
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new BinaryMessage(ByteBuffer.wrap(event.getAudioData())));
            } catch (IOException e) {
                log.error("转发AI音频失败, sessionId={}", event.getSessionId(), e);
            }
        }
    }

    @EventListener
    public void onAiSubtitleDelta(AiSubtitleDeltaEvent event) {
        sendTextToFrontend(event.getSessionId(), Map.of("type", "ai_subtitle", "text", event.getText()));
    }

    @EventListener
    public void onAiSubtitleComplete(AiSubtitleCompleteEvent event) {
        sendTextToFrontend(event.getSessionId(), Map.of("type", "ai_subtitle_complete", "text", event.getText()));
    }

    @EventListener
    public void onUserTranscriptComplete(UserTranscriptCompleteEvent event) {
        sendTextToFrontend(event.getSessionId(), Map.of(
                "type", "user_subtitle",
                "turnId", event.getTurnId(),
                "text", event.getText()
        ));
    }

    @EventListener
    public void onPronunciationEvaluated(PronunciationEvaluatedEvent event) {
        PronunciationResult r = event.getResult();
        sendTextToFrontend(r.sessionId(), Map.ofEntries(
                Map.entry("type", "pronunciation_score"),
                Map.entry("turnId", r.turnId() == null ? "" : r.turnId()),
                Map.entry("text", r.text()),
                Map.entry("suggestedScore", String.valueOf(r.suggestedScore())),
                Map.entry("pronAccuracy", String.valueOf(r.pronAccuracy())),
                Map.entry("pronFluency", String.valueOf(r.pronFluency())),
                Map.entry("pronCompletion", String.valueOf(r.pronCompletion())),
                Map.entry("suggestedGrade", r.suggestedGrade()),
                Map.entry("accuracyGrade", r.accuracyGrade()),
                Map.entry("fluencyGrade", r.fluencyGrade()),
                Map.entry("completionGrade", r.completionGrade())
        ));
    }

    @EventListener
    public void onGrammarCorrected(GrammarCorrectedEvent event) {
        sendTextToFrontend(event.getSessionId(), Map.of(
                "type", "grammar_correction",
                "turnId", event.getTurnId() == null ? "" : event.getTurnId(),
                "text", event.getCorrectedText()
        ));
    }

    // ===== WebSocket 生命周期 =====

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = UUID.randomUUID().toString();
        session.getAttributes().put("sessionId", sessionId);
        sessionMap.put(sessionId, session);
        log.info("前端WebSocket连接已建立, sessionId={}", sessionId);

        // 异步入库：创建会话记录
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            try {
                chatSessionService.startSession(userId, "default", "medium", "us", sessionId);
            } catch (Exception e) {
                log.error("创建会话记录失败, sessionId={}", sessionId, e);
            }
        }

        RealtimeChatClient client = clientFactory.create(sessionId);
        clientMap.put(sessionId, client);
        client.connect();
    }

    private RealtimeChatClient getOrCreateClient(WebSocketSession session) throws Exception {
        String sessionId = (String) session.getAttributes().get("sessionId");
        RealtimeChatClient client = clientMap.get(sessionId);
        if (client == null || !client.isConnected()) {
            if (client != null) {
                try { client.close(); } catch (Exception ignored) {}
            }
            client = clientFactory.create(sessionId);
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
                        String prompt = json.has("prompt") ? json.get("prompt").asText() : "请简洁描述这张图片的内容";
                        if (!image.isEmpty()) {
                            int commaIdx = image.indexOf(',');
                            String pureBase64 = commaIdx >= 0 ? image.substring(commaIdx + 1) : image;
                            client.sendImage(pureBase64, prompt);
                        }
                    }
                }
                case "config" -> {
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
        sessionMap.remove(sessionId);
        visionEnabledMap.remove(sessionId);
        RealtimeChatClient client = clientMap.remove(sessionId);
        if (client != null) {
            client.clearCurrentTurn();
            client.close();
        }
        // 结束会话记录
        try {
            chatSessionService.endSession(sessionId, null);
        } catch (Exception e) {
            log.error("结束会话记录失败, sessionId={}", sessionId, e);
        }
        log.info("前端WebSocket连接已关闭, sessionId={}", sessionId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误", exception);
        afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
    }

    // ===== 私有方法 =====

    private void sendTextToFrontend(String sessionId, Map<String, String> data) {
        WebSocketSession session = sessionMap.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(data)));
            } catch (IOException e) {
                log.error("发送前端失败, sessionId={}", sessionId, e);
            }
        }
    }

    static String buildInstructions(String lang, String role, boolean hasVision) {
        if ("zh".equalsIgnoreCase(lang)) {
            if (hasVision) {
                return "你是一名" + role + "，图片信息是你眼镜所看到的内容。请简洁回答并适时纠正用户的英语。";
            }
            return "你是一名" + role + "。请简洁回答并适时纠正用户的英语。";
        }
        if (hasVision) {
            return "You are " + role + ", the screenshot is what you see with your eyes. Answer concisely and correct the user's English when appropriate.";
        }
        return "You are " + role + ". Answer concisely and correct the user's English when appropriate.";
    }
}

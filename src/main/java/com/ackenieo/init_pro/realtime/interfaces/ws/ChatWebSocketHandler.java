package com.ackenieo.init_pro.realtime.interfaces.ws;

import com.ackenieo.init_pro.conversation.application.service.SessionFinalizeAppService;
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
import com.ackenieo.init_pro.realtime.domain.model.FrontendImage;
import com.ackenieo.init_pro.realtime.domain.model.FrontendImageQueue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    private final Map<String, RealtimeChatClient> clientMap = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> visionEnabledMap = new ConcurrentHashMap<>();
    private final Map<String, FrontendImageQueue> imageQueueMap = new ConcurrentHashMap<>();
    private final Map<String, SessionMetrics> metricsMap = new ConcurrentHashMap<>();
    private final ConversationMemoryService memoryService;
    private final RealtimeChatClientFactory clientFactory;
    private final PromptTemplateService promptTemplateService;
    private final ChatSessionService chatSessionService;
    private final SessionFinalizeAppService sessionFinalizeAppService;

    public ChatWebSocketHandler(ConversationMemoryService memoryService,
                                RealtimeChatClientFactory clientFactory,
                                PromptTemplateService promptTemplateService,
                                ChatSessionService chatSessionService,
                                SessionFinalizeAppService sessionFinalizeAppService) {
        this.memoryService = memoryService;
        this.clientFactory = clientFactory;
        this.promptTemplateService = promptTemplateService;
        this.chatSessionService = chatSessionService;
        this.sessionFinalizeAppService = sessionFinalizeAppService;
    }

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
        SessionMetrics metrics = metricsMap.computeIfAbsent(event.getSessionId(), ignored -> new SessionMetrics());
        metrics.userTurnCount++;
        sendTextToFrontend(event.getSessionId(), Map.of(
                "type", "user_subtitle",
                "turnId", event.getTurnId(),
                "text", event.getText()
        ));
    }

    @EventListener
    public void onPronunciationEvaluated(PronunciationEvaluatedEvent event) {
        PronunciationResult r = event.getResult();
        SessionMetrics metrics = metricsMap.computeIfAbsent(r.sessionId(), ignored -> new SessionMetrics());
        metrics.lastSuggestedGrade = blankToDefault(r.suggestedGrade(), "-");
        metrics.accuracyGrade = blankToDefault(r.accuracyGrade(), "-");
        metrics.fluencyGrade = blankToDefault(r.fluencyGrade(), "-");
        metrics.completionGrade = blankToDefault(r.completionGrade(), "-");
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
        SessionMetrics metrics = metricsMap.computeIfAbsent(event.getSessionId(), ignored -> new SessionMetrics());
        if (event.getCorrectedText() != null && !event.getCorrectedText().isBlank()) {
            metrics.grammarCorrections.add(event.getCorrectedText());
        }
        sendTextToFrontend(event.getSessionId(), Map.of(
                "type", "grammar_correction",
                "turnId", event.getTurnId() == null ? "" : event.getTurnId(),
                "text", event.getCorrectedText()
        ));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String requestedSessionId = getQueryParam(session, "sessionId");
        String reconnectSessionId = getQueryParam(session, "reconnectSessionId");
        String sessionId = firstNonBlank(reconnectSessionId, requestedSessionId);
        boolean reconnect = sessionId != null && chatSessionService.findById(sessionId).isPresent();

        if (!reconnect) {
            sessionId = UUID.randomUUID().toString();
            String userId = (String) session.getAttributes().get("userId");
            if (userId != null) {
                try {
                    chatSessionService.startSession(userId, "default", "medium", "us", sessionId);
                } catch (Exception e) {
                    log.error("创建会话记录失败, sessionId={}", sessionId, e);
                }
            }
            metricsMap.put(sessionId, new SessionMetrics());
            imageQueueMap.put(sessionId, new FrontendImageQueue());
        } else {
            metricsMap.computeIfAbsent(sessionId, ignored -> new SessionMetrics());
            imageQueueMap.computeIfAbsent(sessionId, ignored -> new FrontendImageQueue());
        }

        session.getAttributes().put("sessionId", sessionId);
        WebSocketSession oldSession = sessionMap.put(sessionId, session);
        if (oldSession != null && oldSession.isOpen() && oldSession != session) {
            try {
                oldSession.close(CloseStatus.SESSION_NOT_RELIABLE);
            } catch (IOException e) {
                log.warn("关闭旧连接失败, sessionId={}", sessionId, e);
            }
        }

        RealtimeChatClient client = reconnect ? getOrCreateClient(session) : clientFactory.create(sessionId);
        clientMap.put(sessionId, client);
        if (!client.isConnected()) {
            client.connect();
        }

        log.info("前端WebSocket连接已建立, sessionId={}, reconnect={}", sessionId, reconnect);
        sendLifecycleMessage(sessionId, reconnect ? "reconnected" : "connected", Map.of(
                "sessionId", sessionId,
                "reconnect", reconnect
        ));
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

        if (client != null && !client.isConnected()) {
            log.warn("百炼连接已断开，尝试重连, sessionId={}", sessionId);
            try { client.close(); } catch (Exception ignored) {}
            client = clientFactory.create(sessionId);
            clientMap.put(sessionId, client);
            client.connect();
            if (!client.waitReady()) {
                log.warn("百炼重连未就绪，丢弃音频, sessionId={}", sessionId);
                return;
            }
        }

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
                case "screenshot" -> handleScreenshotMessage(sessionId, client, json);
                case "config" -> handleConfigMessage(session, json, sessionId);
                case "start" -> {
                    RealtimeChatClient ensuredClient = getOrCreateClient(session);
                    log.info("开始通话, connected={}", ensuredClient.isConnected());
                }
                case "finish" -> finishSession(sessionId, true);
                case "reconnect" -> handleReconnectMessage(session, json);
                case "ping" -> session.sendMessage(new PongMessage());
                default -> log.debug("忽略未知消息类型, sessionId={}, type={}", sessionId, type);
            }
        } catch (Exception e) {
            log.warn("解析消息失败", e);
        }
    }

    private void handleScreenshotMessage(String sessionId, RealtimeChatClient client, JsonNode json) {
        String image = json.has("image") ? json.get("image").asText() : "";
        if (image.isBlank()) {
            return;
        }

        String prompt = json.has("prompt") ? json.get("prompt").asText() : "请简洁描述这张图片的内容";
        FrontendImageQueue imageQueue = imageQueueMap.computeIfAbsent(sessionId, ignored -> new FrontendImageQueue());
        imageQueue.enqueueAndTail(normalizeBase64Image(image), prompt);
        log.info("前端图片已入队, sessionId={}, queueSize={}", sessionId, imageQueue.size());

        if (client == null || !client.isConnected()) {
            return;
        }
        if (!memoryService.hasVisualKeywordInLatestUserText(sessionId)) {
            log.info("图片脱敏拦截: sessionId={}, 最新用户文本无视觉关键词", sessionId);
            return;
        }

        FrontendImage imageToSend = imageQueue.tail();
        if (imageToSend != null) {
            client.sendImage(imageToSend.base64Image(), imageToSend.prompt());
        }
    }

    private String normalizeBase64Image(String image) {
        int commaIdx = image.indexOf(',');
        return commaIdx >= 0 ? image.substring(commaIdx + 1) : image;
    }

    private void handleConfigMessage(WebSocketSession session, JsonNode json, String sessionId) throws Exception {
        if (json.has("vision")) {
            String vision = json.get("vision").asText();
            boolean hasVision = "on".equalsIgnoreCase(vision);
            visionEnabledMap.put(sessionId, hasVision);
            log.info("视觉状态变更, sessionId={}, vision={}", sessionId, vision);
        }

        String scene = json.has("scene") ? json.get("scene").asText() : "default";
        String difficulty = json.has("difficulty") ? json.get("difficulty").asText() : "medium";
        String accent = json.has("accent") ? json.get("accent").asText() : "us";
        String instructions;
        if (json.has("lang") || json.has("role")) {
            String lang = json.has("lang") ? json.get("lang").asText() : "en";
            String role = json.has("role") ? json.get("role").asText() : "English Coach";
            boolean hasVision = visionEnabledMap.getOrDefault(sessionId, false);
            instructions = buildInstructions(lang, role, hasVision);
            log.info("收到配置更新(前端格式), sessionId={}, lang={}, role={}, vision={}", sessionId, lang, role, hasVision);
        } else {
            instructions = promptTemplateService.getSystemPrompt(scene, Map.of(
                    "difficulty", difficulty,
                    "accent", accent
            ));
            log.info("收到配置更新(扩展格式), sessionId={}, scene={}, difficulty={}, accent={}", sessionId, scene, difficulty, accent);
        }

        chatSessionService.updateSessionConfig(sessionId, scene, difficulty, accent);
        RealtimeChatClient ensuredClient = getOrCreateClient(session);
        ensuredClient.setInstructions(instructions);
        sendLifecycleMessage(sessionId, "config_updated", Map.of(
                "sessionId", sessionId,
                "scene", scene,
                "difficulty", difficulty,
                "accent", accent,
                "vision", visionEnabledMap.getOrDefault(sessionId, false)
        ));
    }

    private void handleReconnectMessage(WebSocketSession session, JsonNode json) throws Exception {
        String requestedSessionId = json.has("sessionId") ? json.get("sessionId").asText() : "";
        if (requestedSessionId.isBlank() || chatSessionService.findById(requestedSessionId).isEmpty()) {
            sendLifecycleMessage((String) session.getAttributes().get("sessionId"), "reconnect_failed", Map.of(
                    "reason", "session_not_found"
            ));
            return;
        }

        String currentSessionId = (String) session.getAttributes().get("sessionId");
        if (currentSessionId != null && !currentSessionId.equals(requestedSessionId)) {
            sessionMap.remove(currentSessionId, session);
            visionEnabledMap.remove(currentSessionId);
            imageQueueMap.remove(currentSessionId);
        }

        session.getAttributes().put("sessionId", requestedSessionId);
        sessionMap.put(requestedSessionId, session);
        metricsMap.computeIfAbsent(requestedSessionId, ignored -> new SessionMetrics());
        imageQueueMap.computeIfAbsent(requestedSessionId, ignored -> new FrontendImageQueue());
        RealtimeChatClient client = getOrCreateClient(session);
        clientMap.put(requestedSessionId, client);
        sendLifecycleMessage(requestedSessionId, "reconnected", Map.of(
                "sessionId", requestedSessionId,
                "reconnect", true
        ));
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) {
        String sessionId = (String) session.getAttributes().get("sessionId");
        sendLifecycleMessage(sessionId, "pong", Map.of("sessionId", sessionId));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        finishSession((String) session.getAttributes().get("sessionId"), false);
    }

    private void finishSession(String sessionId, boolean closeSocket) throws Exception {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        WebSocketSession socket = sessionMap.get(sessionId);
        SessionMetrics metrics = metricsMap.getOrDefault(sessionId, new SessionMetrics());
        SessionFinalizeAppService.SessionFinalizeResult finalizeResult = null;
        try {
            finalizeResult = sessionFinalizeAppService.finalizeSession(sessionId, metrics.grammarCorrections);
        } catch (Exception e) {
            log.error("结束会话记录失败, sessionId={}", sessionId, e);
        }

        sendSessionEnd(sessionId, finalizeResult, metrics);

        if (closeSocket && socket != null && socket.isOpen()) {
            socket.close(CloseStatus.NORMAL);
        }

        sessionMap.remove(sessionId);
        visionEnabledMap.remove(sessionId);
        imageQueueMap.remove(sessionId);
        RealtimeChatClient client = clientMap.remove(sessionId);
        if (client != null) {
            client.clearCurrentTurn();
            client.close();
        }
        metricsMap.remove(sessionId);
        log.info("前端WebSocket连接已关闭, sessionId={}", sessionId);
    }

    private void sendSessionEnd(String sessionId,
                                SessionFinalizeAppService.SessionFinalizeResult finalizeResult,
                                SessionMetrics metrics) {
        long durationSeconds = finalizeResult == null ? 0L : finalizeResult.durationSeconds();
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "session_end");
        payload.put("sessionId", sessionId);
        payload.put("durationSeconds", durationSeconds);
        payload.put("turnCount", finalizeResult == null ? metrics.userTurnCount : finalizeResult.messageCount());
        payload.put("overallGrade", finalizeResult == null ? blankToDefault(metrics.lastSuggestedGrade, "-") : finalizeResult.overallGrade());
        payload.put("accuracyGrade", finalizeResult == null ? blankToDefault(metrics.accuracyGrade, "-") : finalizeResult.accuracyGrade());
        payload.put("fluencyGrade", finalizeResult == null ? blankToDefault(metrics.fluencyGrade, "-") : finalizeResult.fluencyGrade());
        payload.put("completionGrade", finalizeResult == null ? blankToDefault(metrics.completionGrade, "-") : finalizeResult.completenessGrade());
        payload.put("summary", buildSummary(durationSeconds, metrics));
        payload.put("suggestions", finalizeResult == null
                ? metrics.grammarCorrections.stream().limit(5).toList()
                : splitSuggestions(finalizeResult.suggestion()));
        sendRawTextToFrontend(sessionId, payload);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误", exception);
        finishSession((String) session.getAttributes().get("sessionId"), false);
    }

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

    private void sendLifecycleMessage(String sessionId, String type, Map<String, ?> extra) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", type);
        data.putAll(extra);
        sendRawTextToFrontend(sessionId, data);
    }

    private void sendRawTextToFrontend(String sessionId, Map<String, ?> data) {
        WebSocketSession session = sessionMap.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(data)));
            } catch (IOException e) {
                log.error("发送前端失败, sessionId={}", sessionId, e);
            }
        }
    }

    private static String buildSummary(long durationSeconds, SessionMetrics metrics) {
        long minutes = durationSeconds / 60;
        long seconds = durationSeconds % 60;
        return String.format("本次对话 %02d:%02d，共 %d 轮，总评 %s。", minutes, seconds, metrics.userTurnCount, blankToDefault(metrics.lastSuggestedGrade, "-"));
    }

    private List<String> splitSuggestions(String suggestion) {
        if (suggestion == null || suggestion.isBlank()) {
            return List.of();
        }
        List<String> items = new ArrayList<>();
        for (String line : suggestion.replace("\r", "").split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                items.add(trimmed);
            }
        }
        return items;
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String getQueryParam(WebSocketSession session, String key) {
        if (session.getUri() == null || session.getUri().getQuery() == null) {
            return null;
        }
        String[] pairs = session.getUri().getQuery().split("&");
        for (String pair : pairs) {
            String[] entry = pair.split("=", 2);
            if (entry.length == 2 && key.equals(entry[0])) {
                return entry[1];
            }
        }
        return null;
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

    private static final class SessionMetrics {
        private int userTurnCount;
        private String lastSuggestedGrade = "-";
        private String accuracyGrade = "-";
        private String fluencyGrade = "-";
        private String completionGrade = "-";
        private final List<String> grammarCorrections = new ArrayList<>();
    }
}

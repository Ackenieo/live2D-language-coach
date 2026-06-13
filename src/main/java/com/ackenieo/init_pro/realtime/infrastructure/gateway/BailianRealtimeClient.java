package com.ackenieo.init_pro.realtime.infrastructure.gateway;

import com.ackenieo.init_pro.conversation.domain.service.ConversationMemoryService;
import com.ackenieo.init_pro.evaluation.domain.entity.PronunciationResult;
import com.ackenieo.init_pro.evaluation.domain.gateway.EvaluationResultPublisher;
import com.ackenieo.init_pro.evaluation.domain.service.AudioTurnBufferService;
import com.ackenieo.init_pro.evaluation.domain.service.EvaluationService;
import com.ackenieo.init_pro.realtime.domain.gateway.RealtimeChatClient;
import com.ackenieo.init_pro.realtime.domain.gateway.RealtimeChatClientFactory;
import com.ackenieo.init_pro.realtime.infrastructure.config.BailianConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 百炼 Realtime API 客户端
 * 同时实现 EvaluationResultPublisher，将评估结果通过 WebSocket 推送到前端
 */
public class BailianRealtimeClient extends WebSocketClient implements RealtimeChatClient, EvaluationResultPublisher {
    private static final Logger log = LoggerFactory.getLogger(BailianRealtimeClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicBoolean connected = new AtomicBoolean(false);

    private final WebSocketSession frontendSession;
    private final ConversationMemoryService memoryService;
    private final AudioTurnBufferService audioTurnBufferService;
    private final EvaluationService evaluationService;
    private final BailianConfig bailianConfig;
    private final String sessionId;
    private final CountDownLatch readyLatch = new CountDownLatch(1);
    private volatile String currentTurnId;
    private volatile String instructions;

    public BailianRealtimeClient(WebSocketSession frontendSession,
                                  String sessionId,
                                  ConversationMemoryService memoryService,
                                  AudioTurnBufferService audioTurnBufferService,
                                  EvaluationService evaluationService,
                                  BailianConfig bailianConfig) throws Exception {
        super(new URI(bailianConfig.getWsUrl()), new Draft_6455(), buildHeaders(bailianConfig), 0);
        this.frontendSession = frontendSession;
        this.sessionId = sessionId;
        this.memoryService = memoryService;
        this.audioTurnBufferService = audioTurnBufferService;
        this.evaluationService = evaluationService;
        this.bailianConfig = bailianConfig;
        this.instructions = buildDefaultInstructions();
    }

    private static String buildDefaultInstructions() {
        return "You are an English speaking coach. Answer concisely and correct the user's English when appropriate.";
    }

    @Override
    public void setInstructions(String instructions) {
        this.instructions = instructions;
        if (isOpen()) {
            sendSessionUpdate();
        }
        log.info("已切换系统指令, sessionId={}, instructions={}", sessionId,
                instructions.length() > 80 ? instructions.substring(0, 80) + "..." : instructions);
    }

    private void sendSessionUpdate() {
        try {
            String escapedInstructions = instructions
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n");
            String msg = "{\"type\":\"session.update\",\"session\":{\"instructions\":\"" +
                    escapedInstructions + "\",\"modalities\":[\"text\",\"audio\"],\"voice\":\"" +
                    bailianConfig.getVoice() + "\"}}";
            send(msg);
            log.info("已发送session.update, sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("发送session.update失败", e);
        }
    }

    private static Map<String, String> buildHeaders(BailianConfig config) {
        return Map.of("Authorization", "Bearer " + config.getApiKey());
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        connected.set(true);
        log.info("百炼连接已建立");
    }

    @Override
    public void onMessage(String message) {
        long receiveAt = System.currentTimeMillis();
        try {
            JsonNode json = objectMapper.readTree(message);
            String type = json.get("type").asText();
            log.info("收到百炼消息, type={}, costMs={}", type, System.currentTimeMillis() - receiveAt);

            switch (type) {
                case "session.created" -> {
                    log.info("会话就绪(type=session.created)");
                    sendSessionUpdate();
                    readyLatch.countDown();
                }
                case "input_audio_buffer.speech_started" -> handleSpeechStarted();
                case "input_audio_buffer.speech_stopped" -> log.info("检测到语音停止, 等待百炼回复");
                case "input_audio_buffer.committed" -> log.info("音频已提交");
                case "conversation.item.created" -> log.info("对话项已创建, 准备生成回复");
                case "response.audio.delta" -> {
                    byte[] audio = Base64.getDecoder().decode(json.get("delta").asText());
                    if (frontendSession.isOpen()) {
                        frontendSession.sendMessage(new BinaryMessage(ByteBuffer.wrap(audio)));
                    }
                }
                case "response.audio_transcript.delta" -> {
                    String text = json.get("delta").asText();
                    sendToFrontend(Map.of("type", "ai_subtitle", "text", text));
                }
                case "response.audio_transcript.done" -> {
                    String text = json.get("transcript").asText();
                    sendToFrontend(Map.of("type", "ai_subtitle_complete", "text", text));
                    if (memoryService != null) {
                        memoryService.saveMessage(sessionId, "assistant", text);
                    }
                }
                case "response.done" -> log.info("响应完成");
                case "conversation.item.input_audio_transcription.completed" -> {
                    String text = json.has("transcript") ? json.get("transcript").asText() : "";
                    if (!text.isEmpty()) {
                        String turnId = currentTurnId;
                        sendToFrontend(Map.of("type", "user_subtitle", "turnId", safeTurnId(turnId), "text", text));
                        if (memoryService != null) {
                            memoryService.saveMessage(sessionId, "user", text);
                        }
                        if (evaluationService != null) {
                            evaluationService.evaluateTurn(turnId, sessionId, audioTurnBufferService, this, text);
                            currentTurnId = null;
                        }
                    }
                }
                case "error" -> log.error("百炼错误: {}", message);
            }
        } catch (Exception e) {
            log.error("处理消息失败", e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        connected.set(false);
        log.info("百炼连接已关闭, code={}, reason={}, remote={}", code, reason, remote);
    }

    @Override
    public void onError(Exception ex) {
        connected.set(false);
        log.error("百炼错误", ex);
    }

    @Override
    public boolean isConnected() {
        return connected.get() && isOpen();
    }

    @Override
    public boolean waitReady() {
        try {
            return readyLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    @Override
    public void sendAudio(byte[] data) {
        if (currentTurnId != null && !currentTurnId.isBlank()) {
            audioTurnBufferService.appendAudio(currentTurnId, sessionId, data);
        }

        if (isOpen()) {
            try {
                String audioBase64 = Base64.getEncoder().encodeToString(data);
                send("{\"type\":\"input_audio_buffer.append\",\"audio\":\"" + audioBase64 + "\"}");
            } catch (Exception e) {
                log.error("发送音频失败", e);
            }
        }
    }

    @Override
    public void sendText(String text) {
        if (!isOpen() || !waitReady()) {
            log.warn("未就绪");
            return;
        }
        try {
            byte[] silence = new byte[3200];
            String silenceBase64 = Base64.getEncoder().encodeToString(silence);
            send("{\"type\":\"input_audio_buffer.append\",\"audio\":\"" + silenceBase64 + "\"}");
            send("{\"type\":\"input_audio_buffer.commit\"}");
            send("{\"type\":\"response.create\"}");
            log.info("已发送文本: {}", text);
        } catch (Exception e) {
            log.error("发送文本失败", e);
        }
    }

    @Override
    public void sendImage(String base64Image, String text) {
        if (!isOpen()) {
            log.warn("连接未打开");
            return;
        }
        if (!waitReady()) {
            log.warn("会话未就绪");
            return;
        }
        try {
            send("{\"type\":\"input_image_buffer.append\",\"image\":\"" + base64Image + "\"}");
            log.info("已发送图片(静默注入上下文), sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("发送图片失败", e);
        }
    }

    @Override
    public void clearCurrentTurn() {
        this.currentTurnId = null;
    }

    // ===== EvaluationResultPublisher 实现 =====

    @Override
    public void publishPronunciationResult(PronunciationResult result) {
        try {
            if (frontendSession.isOpen()) {
                frontendSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.ofEntries(
                        Map.entry("type", "pronunciation_score"),
                        Map.entry("turnId", result.turnId() == null ? "" : result.turnId()),
                        Map.entry("text", result.text()),
                        Map.entry("suggestedScore", result.suggestedScore()),
                        Map.entry("pronAccuracy", result.pronAccuracy()),
                        Map.entry("pronFluency", result.pronFluency()),
                        Map.entry("pronCompletion", result.pronCompletion()),
                        Map.entry("suggestedGrade", result.suggestedGrade()),
                        Map.entry("accuracyGrade", result.accuracyGrade()),
                        Map.entry("fluencyGrade", result.fluencyGrade()),
                        Map.entry("completionGrade", result.completionGrade())
                ))));
            }
        } catch (Exception e) {
            log.error("发送发音评分到前端失败", e);
        }
    }

    @Override
    public void publishGrammarCorrection(String turnId, String correctedText) {
        try {
            if (frontendSession.isOpen()) {
                frontendSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                        "type", "grammar_correction",
                        "turnId", turnId == null ? "" : turnId,
                        "text", correctedText
                ))));
            }
        } catch (Exception e) {
            log.error("发送语法纠正结果到前端失败", e);
        }
    }

    // ===== 私有方法 =====

    private void handleSpeechStarted() {
        if (currentTurnId == null || currentTurnId.isBlank()) {
            currentTurnId = audioTurnBufferService.startTurn(sessionId);
        }
        log.info("检测到语音开始, sessionId={}, turnId={}", sessionId, currentTurnId);
    }

    private void sendToFrontend(Map<String, String> data) {
        try {
            if (frontendSession.isOpen()) {
                frontendSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(data)));
            }
        } catch (Exception e) {
            log.error("发送前端失败", e);
        }
    }

    private String safeTurnId(String turnId) {
        return turnId == null ? "" : turnId;
    }

    /**
     * BailianRealtimeClient 工厂实现
     */
    @Component
    public static class Factory implements RealtimeChatClientFactory {
        private final ConversationMemoryService memoryService;
        private final AudioTurnBufferService audioTurnBufferService;
        private final EvaluationService evaluationService;
        private final BailianConfig bailianConfig;

        public Factory(ConversationMemoryService memoryService,
                       AudioTurnBufferService audioTurnBufferService,
                       EvaluationService evaluationService,
                       BailianConfig bailianConfig) {
            this.memoryService = memoryService;
            this.audioTurnBufferService = audioTurnBufferService;
            this.evaluationService = evaluationService;
            this.bailianConfig = bailianConfig;
        }

        @Override
        public RealtimeChatClient create(WebSocketSession frontendSession, String sessionId) {
            try {
                return new BailianRealtimeClient(
                        frontendSession, sessionId,
                        memoryService, audioTurnBufferService,
                        evaluationService, bailianConfig);
            } catch (Exception e) {
                throw new RuntimeException("创建百炼客户端失败", e);
            }
        }
    }
}

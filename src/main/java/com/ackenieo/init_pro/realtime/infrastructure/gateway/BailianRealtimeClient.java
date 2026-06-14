package com.ackenieo.init_pro.realtime.infrastructure.gateway;

import com.ackenieo.init_pro.conversation.domain.service.ConversationMemoryService;
import com.ackenieo.init_pro.evaluation.domain.service.AudioTurnBufferService;
import com.ackenieo.init_pro.evaluation.domain.service.EvaluationService;
import com.ackenieo.init_pro.realtime.domain.event.AiAudioDoneEvent;
import com.ackenieo.init_pro.realtime.domain.event.AiAudioDeltaEvent;
import com.ackenieo.init_pro.realtime.domain.event.AiSubtitleCompleteEvent;
import com.ackenieo.init_pro.realtime.domain.event.AiSubtitleDeltaEvent;
import com.ackenieo.init_pro.realtime.domain.event.UserTranscriptCompleteEvent;
import com.ackenieo.init_pro.realtime.domain.gateway.RealtimeChatClient;
import com.ackenieo.init_pro.realtime.domain.gateway.RealtimeChatClientFactory;
import com.ackenieo.init_pro.realtime.domain.model.SessionState;
import com.ackenieo.init_pro.realtime.domain.model.VoiceConfig;
import com.ackenieo.init_pro.realtime.infrastructure.config.BailianConfig;
import com.ackenieo.init_pro.shared.domain.DomainEventPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 百炼 Realtime API 客户端
 * 通过 DomainEventPublisher 发布领域事件，使用 SessionState/VoiceConfig 值对象管理状态
 */
public class BailianRealtimeClient extends WebSocketClient implements RealtimeChatClient {
    private static final Logger log = LoggerFactory.getLogger(BailianRealtimeClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String sessionId;
    private final ConversationMemoryService memoryService;
    private final AudioTurnBufferService audioTurnBufferService;
    private final EvaluationService evaluationService;
    private final DomainEventPublisher eventPublisher;
    private final BailianConfig bailianConfig;
    private final CountDownLatch readyLatch = new CountDownLatch(1);
    private final AtomicReference<SessionState> sessionState = new AtomicReference<>(new SessionState());
    private final AtomicReference<VoiceConfig> voiceConfig = new AtomicReference<>(VoiceConfig.DEFAULT);
    private volatile String instructions;

    public BailianRealtimeClient(String sessionId,
                                  ConversationMemoryService memoryService,
                                  AudioTurnBufferService audioTurnBufferService,
                                  EvaluationService evaluationService,
                                  DomainEventPublisher eventPublisher,
                                  BailianConfig bailianConfig) throws Exception {
        super(new URI(bailianConfig.getWsUrl()), new Draft_6455(), buildHeaders(bailianConfig), 0);
        this.sessionId = sessionId;
        this.memoryService = memoryService;
        this.audioTurnBufferService = audioTurnBufferService;
        this.evaluationService = evaluationService;
        this.eventPublisher = eventPublisher;
        this.bailianConfig = bailianConfig;
        this.instructions = buildDefaultInstructions();
        this.voiceConfig.set(new VoiceConfig(bailianConfig.getVoice(), Set.of("text", "audio")));
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
            VoiceConfig vc = voiceConfig.get();
            String escapedInstructions = instructions
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n");

            StringBuilder modalitiesJson = new StringBuilder("[");
            int i = 0;
            for (String modality : vc.getModalities()) {
                if (i++ > 0) modalitiesJson.append(",");
                modalitiesJson.append("\"").append(modality).append("\"");
            }
            modalitiesJson.append("]");

            String msg = "{\"type\":\"session.update\",\"session\":{\"instructions\":\"" +
                    escapedInstructions + "\",\"modalities\":" + modalitiesJson +
                    ",\"voice\":\"" + vc.getVoiceName() + "\"}}";
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
        log.info("百炼连接已建立, sessionId={}", sessionId);
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
                    sessionState.updateAndGet(s -> s.withStatus(SessionState.Status.READY));
                    log.info("会话就绪(type=session.created), state={}", sessionState.get());
                    sendSessionUpdate();
                    readyLatch.countDown();
                }
                case "input_audio_buffer.speech_started" -> handleSpeechStarted();
                case "input_audio_buffer.speech_stopped" -> {
                    sessionState.updateAndGet(s -> s.withStatus(SessionState.Status.PROCESSING));
                    log.info("检测到语音停止, state={}", sessionState.get());
                }
                case "input_audio_buffer.committed" -> log.info("音频已提交");
                case "conversation.item.created" -> log.info("对话项已创建, 准备生成回复");
                case "response.audio.delta" -> {
                    String delta = json.has("delta") ? json.get("delta").asText() : "";
                    if (!delta.isBlank()) {
                        byte[] audio = Base64.getDecoder().decode(delta);
                        eventPublisher.publish(new AiAudioDeltaEvent(
                                sessionId, audio, responseId(json), itemId(json)));
                    }
                }
                case "response.audio.done" -> {
                    eventPublisher.publish(new AiAudioDoneEvent(sessionId, responseId(json), itemId(json)));
                }
                case "response.audio_transcript.delta" -> {
                    String text = json.get("delta").asText();
                    eventPublisher.publish(new AiSubtitleDeltaEvent(sessionId, text, responseId(json), itemId(json)));
                }
                case "response.audio_transcript.done" -> {
                    String text = json.get("transcript").asText();
                    eventPublisher.publish(new AiSubtitleCompleteEvent(sessionId, text, responseId(json), itemId(json)));
                    if (memoryService != null) {
                        memoryService.saveMessage(sessionId, "assistant", text);
                    }
                }
                case "response.done" -> {
                    sessionState.updateAndGet(s -> s.withStatus(SessionState.Status.READY));
                    log.info("响应完成, state={}", sessionState.get());
                }
                case "conversation.item.input_audio_transcription.completed" -> {
                    String text = json.has("transcript") ? json.get("transcript").asText() : "";
                    if (!text.isEmpty()) {
                        String turnId = sessionState.get().getTurnId();
                        eventPublisher.publish(new UserTranscriptCompleteEvent(sessionId, safeTurnId(turnId), text));
                        if (memoryService != null) {
                            memoryService.saveMessage(sessionId, "user", text);
                        }
                        if (evaluationService != null) {
                            evaluationService.evaluateTurn(turnId, sessionId, audioTurnBufferService, text);
                            sessionState.updateAndGet(s -> s.withTurnId(null));
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
        sessionState.updateAndGet(s -> s.withStatus(SessionState.Status.CLOSED));
        log.info("百炼连接已关闭, sessionId={}, state={}", sessionId, sessionState.get());
    }

    @Override
    public void onError(Exception ex) {
        sessionState.updateAndGet(s -> s.withStatus(SessionState.Status.CLOSED));
        log.error("百炼错误, sessionId={}, state={}", sessionId, sessionState.get(), ex);
    }

    @Override
    public boolean isConnected() {
        return sessionState.get().isActive() && isOpen();
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
        String turnId = sessionState.get().getTurnId();
        if (turnId != null && !turnId.isBlank()) {
            audioTurnBufferService.appendAudio(turnId, sessionId, data);
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
    public boolean sendImage(String base64Image) {
        if (!isOpen()) {
            log.warn("连接未打开");
            return false;
        }
        if (!waitReady()) {
            log.warn("会话未就绪");
            return false;
        }
        try {
            send(buildImageAppendMessage(base64Image));
            log.info("已发送图片上下文, sessionId={}", sessionId);
            return true;
        } catch (Exception e) {
            log.error("发送图片失败", e);
            return false;
        }
    }

    static String buildImageAppendMessage(String base64Image) throws Exception {
        ObjectNode msg = objectMapper.createObjectNode();
        msg.put("type", "input_image_buffer.append");
        msg.put("image", base64Image);
        return objectMapper.writeValueAsString(msg);
    }

    @Override
    public void clearCurrentTurn() {
        sessionState.updateAndGet(s -> s.withTurnId(null));
    }

    private void handleSpeechStarted() {
        sessionState.updateAndGet(s -> {
            if (s.getTurnId() == null || s.getTurnId().isBlank()) {
                String newTurnId = audioTurnBufferService.startTurn(sessionId);
                return s.withStatus(SessionState.Status.SPEAKING).withTurnId(newTurnId);
            }
            return s.withStatus(SessionState.Status.SPEAKING);
        });
        log.info("检测到语音开始, sessionId={}, state={}", sessionId, sessionState.get());
    }

    private String safeTurnId(String turnId) {
        return turnId == null ? "" : turnId;
    }

    private static String responseId(JsonNode json) {
        return firstText(json, "response_id", "responseId");
    }

    private static String itemId(JsonNode json) {
        return firstText(json, "item_id", "itemId");
    }

    private static String firstText(JsonNode json, String... fields) {
        for (String field : fields) {
            if (json.hasNonNull(field)) {
                String value = json.get(field).asText();
                if (!value.isBlank()) {
                    return value;
                }
            }
        }
        return "";
    }

    /**
     * 工厂实现
     */
    @Component
    public static class Factory implements RealtimeChatClientFactory {
        private final ConversationMemoryService memoryService;
        private final AudioTurnBufferService audioTurnBufferService;
        private final EvaluationService evaluationService;
        private final DomainEventPublisher eventPublisher;
        private final BailianConfig bailianConfig;

        public Factory(ConversationMemoryService memoryService,
                       AudioTurnBufferService audioTurnBufferService,
                       EvaluationService evaluationService,
                       DomainEventPublisher eventPublisher,
                       BailianConfig bailianConfig) {
            this.memoryService = memoryService;
            this.audioTurnBufferService = audioTurnBufferService;
            this.evaluationService = evaluationService;
            this.eventPublisher = eventPublisher;
            this.bailianConfig = bailianConfig;
        }

        @Override
        public RealtimeChatClient create(String sessionId) {
            try {
                return new BailianRealtimeClient(
                        sessionId,
                        memoryService, audioTurnBufferService,
                        evaluationService, eventPublisher, bailianConfig);
            } catch (Exception e) {
                throw new RuntimeException("创建百炼客户端失败", e);
            }
        }
    }
}

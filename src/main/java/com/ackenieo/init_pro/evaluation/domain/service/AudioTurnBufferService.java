package com.ackenieo.init_pro.evaluation.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 音频轮次缓冲服务
 */
@Component
public class AudioTurnBufferService {
    private static final Logger log = LoggerFactory.getLogger(AudioTurnBufferService.class);

    private final Map<String, ByteArrayOutputStream> audioBufferMap = new ConcurrentHashMap<>();
    private final Map<String, String> turnSessionMap = new ConcurrentHashMap<>();

    public String startTurn(String sessionId) {
        String turnId = UUID.randomUUID().toString();
        audioBufferMap.put(turnId, new ByteArrayOutputStream());
        turnSessionMap.put(turnId, sessionId);
        log.info("开始收集音频轮次: sessionId={}, turnId={}", sessionId, turnId);
        return turnId;
    }

    public void appendAudio(String turnId, String sessionId, byte[] audioChunk) {
        if (turnId == null || turnId.isBlank() || audioChunk == null || audioChunk.length == 0) {
            return;
        }

        audioBufferMap.compute(turnId, (key, existing) -> {
            ByteArrayOutputStream output = existing != null ? existing : new ByteArrayOutputStream();
            try {
                output.write(audioChunk);
            } catch (Exception e) {
                log.warn("追加音频失败, turnId={}", turnId, e);
            }
            return output;
        });
        turnSessionMap.put(turnId, sessionId);
    }

    public byte[] removeAudio(String turnId) {
        ByteArrayOutputStream output = audioBufferMap.remove(turnId);
        turnSessionMap.remove(turnId);
        return output == null ? new byte[0] : output.toByteArray();
    }

    public String getSessionId(String turnId) {
        return turnSessionMap.get(turnId);
    }

    public void clearTurn(String turnId) {
        audioBufferMap.remove(turnId);
        turnSessionMap.remove(turnId);
    }
}

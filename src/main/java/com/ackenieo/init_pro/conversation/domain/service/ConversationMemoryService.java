package com.ackenieo.init_pro.conversation.domain.service;

import com.ackenieo.init_pro.conversation.domain.repository.ChatMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 对话记忆服务
 */
@Service
public class ConversationMemoryService {
    private static final Logger log = LoggerFactory.getLogger(ConversationMemoryService.class);

    private static final Pattern VISUAL_INTENT_PATTERN = Pattern.compile(
            "看看|看一看|看一下|看见|看到|看清|可视|视觉|画面|图片|拍照|拍一下|能看到|你能看见|你能看到|你看|可见|瞅瞅|瞧瞧|瞄一眼|瞅一眼|望一眼|瞟一眼|瞧"
    );

    private final ChatMemory chatMemory;

    public ConversationMemoryService(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    public void saveMessage(String sessionId, String role, String text) {
        chatMemory.add(sessionId, role, text);
        log.debug("保存对话消息: sessionId={}, role={}", sessionId, role);
    }

    public List<Map<String, String>> getConversationHistory(String sessionId) {
        return chatMemory.get(sessionId);
    }

    public String getConversationSummary(String sessionId) {
        return chatMemory.getSummary(sessionId);
    }

    public void clearConversation(String sessionId) {
        chatMemory.clear(sessionId);
        log.info("清空对话历史: sessionId={}", sessionId);
    }

    public boolean hasVisualKeyword(String text) {
        return text != null && VISUAL_INTENT_PATTERN.matcher(text).find();
    }

    public boolean hasVisualKeywordInLatestUserText(String sessionId) {
        List<Map<String, String>> messages = chatMemory.get(sessionId);
        for (int i = messages.size() - 1; i >= 0; i--) {
            Map<String, String> msg = messages.get(i);
            if ("user".equals(msg.get("role"))) {
                String text = msg.get("text");
                boolean found = hasVisualKeyword(text);
                if (found) {
                    log.info("检测到视觉关键词: sessionId={}, text={}", sessionId, text);
                }
                return found;
            }
        }
        return false;
    }
}

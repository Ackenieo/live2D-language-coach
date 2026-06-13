package com.ackenieo.init_pro.conversation.domain.repository;

import java.util.List;
import java.util.Map;

/**
 * 对话记忆接口
 */
public interface ChatMemory {
    void add(String conversationId, String role, String text);
    List<Map<String, String>> get(String conversationId);
    String getSummary(String conversationId);
    void clear(String conversationId);
}

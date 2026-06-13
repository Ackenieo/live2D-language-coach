package com.ackenieo.init_pro.conversation.interfaces.rest;

import com.ackenieo.init_pro.conversation.domain.service.ChatSessionService;
import com.ackenieo.init_pro.conversation.domain.service.ConversationMemoryService;
import com.ackenieo.init_pro.shared.infrastructure.ApiResponse;
import com.ackenieo.init_pro.shared.infrastructure.BaseController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 对话 REST 控制器
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController extends BaseController {

    private final ChatSessionService chatSessionService;
    private final ConversationMemoryService conversationMemoryService;

    public ChatController(ChatSessionService chatSessionService,
                          ConversationMemoryService conversationMemoryService) {
        this.chatSessionService = chatSessionService;
        this.conversationMemoryService = conversationMemoryService;
    }

    @GetMapping("/sessions/{sessionId}/history")
    public ResponseEntity<ApiResponse<Object>> getHistory(@PathVariable String sessionId) {
        return success(Map.of(
                "sessionId", sessionId,
                "messages", conversationMemoryService.getConversationHistory(sessionId)
        ));
    }

    @DeleteMapping("/sessions/{sessionId}/history")
    public ResponseEntity<ApiResponse<Void>> clearHistory(@PathVariable String sessionId) {
        conversationMemoryService.clearConversation(sessionId);
        return success(null);
    }

    @GetMapping("/sessions/{sessionId}/summary")
    public ResponseEntity<ApiResponse<Object>> getSummary(@PathVariable String sessionId) {
        return success(Map.of(
                "sessionId", sessionId,
                "summary", conversationMemoryService.getConversationSummary(sessionId)
        ));
    }
}

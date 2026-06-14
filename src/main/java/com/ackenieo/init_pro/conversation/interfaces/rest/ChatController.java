package com.ackenieo.init_pro.conversation.interfaces.rest;

import com.ackenieo.init_pro.conversation.application.service.ChatHistoryAppService;
import com.ackenieo.init_pro.conversation.application.service.ReportAppService;
import com.ackenieo.init_pro.conversation.domain.service.ChatSessionService;
import com.ackenieo.init_pro.conversation.domain.service.ConversationMemoryService;
import com.ackenieo.init_pro.conversation.interfaces.dto.ChatHistoryResponse;
import com.ackenieo.init_pro.conversation.interfaces.dto.ChatReportResponse;
import com.ackenieo.init_pro.shared.infrastructure.ApiResponse;
import com.ackenieo.init_pro.shared.infrastructure.BaseController;
import com.ackenieo.init_pro.user.infrastructure.security.CurrentUserConstants;
import jakarta.servlet.http.HttpServletRequest;
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
    private final ReportAppService reportAppService;
    private final ChatHistoryAppService chatHistoryAppService;

    public ChatController(ChatSessionService chatSessionService,
                          ConversationMemoryService conversationMemoryService,
                          ReportAppService reportAppService,
                          ChatHistoryAppService chatHistoryAppService) {
        this.chatSessionService = chatSessionService;
        this.conversationMemoryService = conversationMemoryService;
        this.reportAppService = reportAppService;
        this.chatHistoryAppService = chatHistoryAppService;
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

    @GetMapping("/report/{sessionId}")
    public ResponseEntity<ApiResponse<ChatReportResponse>> getReport(HttpServletRequest request,
                                                                     @PathVariable String sessionId) {
        return success(reportAppService.getReport(currentUserId(request), sessionId));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<ChatHistoryResponse>> getSessionHistory(HttpServletRequest request,
                                                                              @RequestParam(defaultValue = "1") int page,
                                                                              @RequestParam(defaultValue = "20") int pageSize) {
        return success(chatHistoryAppService.getHistory(currentUserId(request), page, pageSize));
    }

    private String currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(CurrentUserConstants.CURRENT_USER_ID);
        if (userId == null) {
            throw new RuntimeException("当前用户未登录");
        }
        return String.valueOf(userId);
    }
}

package com.ackenieo.init_pro.conversation.domain.service;

import com.ackenieo.init_pro.conversation.domain.entity.ChatMessage;
import com.ackenieo.init_pro.conversation.domain.repository.ChatMessageRepository;
import com.ackenieo.init_pro.evaluation.domain.event.PronunciationEvaluatedEvent;
import com.ackenieo.init_pro.realtime.domain.event.AiSubtitleCompleteEvent;
import com.ackenieo.init_pro.realtime.domain.event.UserTranscriptCompleteEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 对话消息异步持久化服务
 * 监听领域事件，异步入库 MySQL，不阻塞对话主流程
 */
@Service
public class ChatMessagePersistenceService {
    private static final Logger log = LoggerFactory.getLogger(ChatMessagePersistenceService.class);

    private final ChatMessageRepository chatMessageRepository;

    public ChatMessagePersistenceService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    /**
     * AI 回复完成 → 异步入库
     */
    @Async
    @EventListener
    public void onAiSubtitleComplete(AiSubtitleCompleteEvent event) {
        try {
            ChatMessage msg = new ChatMessage();
            msg.setSessionId(event.getSessionId());
            msg.setRole("assistant");
            msg.setContent(event.getText());
            chatMessageRepository.save(msg);
            log.debug("AI消息已入库, sessionId={}", event.getSessionId());
        } catch (Exception e) {
            log.error("AI消息入库失败, sessionId={}", event.getSessionId(), e);
        }
    }

    /**
     * 用户语音识别完成 → 异步入库
     */
    @Async
    @EventListener
    public void onUserTranscriptComplete(UserTranscriptCompleteEvent event) {
        try {
            ChatMessage msg = new ChatMessage();
            msg.setSessionId(event.getSessionId());
            msg.setRole("user");
            msg.setContent(event.getText());
            msg.setTurnId(event.getTurnId());
            chatMessageRepository.save(msg);
            log.debug("用户消息已入库, sessionId={}, turnId={}", event.getSessionId(), event.getTurnId());
        } catch (Exception e) {
            log.error("用户消息入库失败, sessionId={}", event.getSessionId(), e);
        }
    }

    /**
     * 发音评测完成 → 更新对应用户消息评分
     */
    @Async
    @EventListener
    public void onPronunciationEvaluated(PronunciationEvaluatedEvent event) {
        try {
            var result = event.getResult();
            chatMessageRepository.findBySessionIdAndTurnId(result.sessionId(), result.turnId()).ifPresent(message -> {
                message.setPronAccuracy(result.pronAccuracy());
                message.setPronFluency(result.pronFluency());
                message.setPronCompletion(result.pronCompletion());
                message.setAccuracyGrade(result.accuracyGrade());
                message.setFluencyGrade(result.fluencyGrade());
                message.setCompletionGrade(result.completionGrade());
                message.markUpdated();
                chatMessageRepository.save(message);
            });
            log.debug("发音评分已更新, sessionId={}, turnId={}", result.sessionId(), result.turnId());
        } catch (Exception e) {
            log.error("发音评分更新失败, eventId={}", event.getEventId(), e);
        }
    }
}

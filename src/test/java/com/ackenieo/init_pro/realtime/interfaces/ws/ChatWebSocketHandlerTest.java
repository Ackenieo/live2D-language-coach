package com.ackenieo.init_pro.realtime.interfaces.ws;

import com.ackenieo.init_pro.conversation.application.service.SessionFinalizeAppService;
import com.ackenieo.init_pro.conversation.domain.service.ChatSessionService;
import com.ackenieo.init_pro.conversation.domain.service.PromptTemplateService;
import com.ackenieo.init_pro.realtime.domain.event.UserTranscriptCompleteEvent;
import com.ackenieo.init_pro.realtime.domain.gateway.RealtimeChatClient;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatWebSocketHandlerTest {

    @Test
    void configMessagesUseSceneTemplatesEvenWhenLegacyRoleFieldsArePresent() throws Exception {
        FakeRealtimeChatClient client = new FakeRealtimeChatClient();
        ChatWebSocketHandler handler = newHandler(client);
        WebSocketSession session = session("session-1");

        handler.handleTextMessage(session, new TextMessage("""
                {"type":"config","lang":"zh","role":"Doctor","scene":"airport","difficulty":"easy","accent":"us"}
                """));

        assertThat(client.instructions).contains("airport check-in agent");
        assertThat(client.instructions).doesNotContain("Doctor");
        assertThat(client.instructions).doesNotContain("{destination}");
    }

    @Test
    void sendsLatestQueuedImageOncePerUserTurnBeforeAudio() throws Exception {
        FakeRealtimeChatClient client = new FakeRealtimeChatClient();
        ChatWebSocketHandler handler = newHandler(client);
        WebSocketSession session = session("session-1");

        handler.handleTextMessage(session, new TextMessage("""
                {"type":"config","vision":"on","scene":"default","difficulty":"medium","accent":"us"}
                """));
        handler.handleTextMessage(session, new TextMessage("""
                {"type":"screenshot","image":"data:image/jpeg;base64,image-1","prompt":"ignored"}
                """));

        handler.handleBinaryMessage(session, new BinaryMessage(new byte[] {1, 2}));
        handler.handleBinaryMessage(session, new BinaryMessage(new byte[] {3, 4}));

        assertThat(client.imageSendCount).isEqualTo(1);
        assertThat(client.lastImage).isEqualTo("image-1");

        handler.onUserTranscriptComplete(new UserTranscriptCompleteEvent("session-1", "turn-1", "What can you see?"));
        handler.handleBinaryMessage(session, new BinaryMessage(new byte[] {5, 6}));

        assertThat(client.imageSendCount).isEqualTo(2);
    }

    private ChatWebSocketHandler newHandler(FakeRealtimeChatClient client) {
        return new ChatWebSocketHandler(
                sessionId -> client,
                new PromptTemplateService(),
                mock(ChatSessionService.class),
                mock(SessionFinalizeAppService.class)
        );
    }

    private WebSocketSession session(String sessionId) {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sessionId", sessionId);
        when(session.getAttributes()).thenReturn(attributes);
        return session;
    }

    private static final class FakeRealtimeChatClient implements RealtimeChatClient {
        private boolean connected;
        private String instructions;
        private int imageSendCount;
        private String lastImage;

        @Override
        public void connect() {
            connected = true;
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public boolean waitReady() {
            return true;
        }

        @Override
        public void sendAudio(byte[] data) {
        }

        @Override
        public void sendText(String text) {
        }

        @Override
        public boolean sendImage(String base64Image) {
            imageSendCount++;
            lastImage = base64Image;
            return true;
        }

        @Override
        public void setInstructions(String instructions) {
            this.instructions = instructions;
        }

        @Override
        public void clearCurrentTurn() {
        }

        @Override
        public void close() {
            connected = false;
        }
    }
}

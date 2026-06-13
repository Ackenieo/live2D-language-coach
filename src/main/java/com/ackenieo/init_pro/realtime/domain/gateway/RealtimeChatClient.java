package com.ackenieo.init_pro.realtime.domain.gateway;

/**
 * 实时对话客户端接口
 */
public interface RealtimeChatClient {
    void connect();
    boolean isConnected();
    boolean waitReady();
    void sendAudio(byte[] data);
    void sendText(String text);
    void sendImage(String base64Image, String text);
    void setInstructions(String instructions);
    void clearCurrentTurn();
    void close();
}

package com.ackenieo.init_pro.realtime.domain.gateway;

/**
 * 实时对话客户端工厂接口
 * 解耦接口层与基础设施实现的直接依赖
 */
public interface RealtimeChatClientFactory {
    /**
     * 为指定会话创建实时对话客户端
     */
    RealtimeChatClient create(String sessionId);
}

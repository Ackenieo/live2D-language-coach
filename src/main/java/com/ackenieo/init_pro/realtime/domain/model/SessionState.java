package com.ackenieo.init_pro.realtime.domain.model;

import com.ackenieo.init_pro.shared.domain.BaseValueObject;

import java.util.Objects;

/**
 * 会话状态值对象
 * 封装实时对话会话的生命周期状态
 */
public class SessionState extends BaseValueObject {
    private final Status status;
    private final String turnId;

    public enum Status {
        CONNECTING,     // 百炼连接中
        READY,          // 会话就绪（session.created 已收到）
        SPEAKING,       // 用户正在说话
        PROCESSING,     // 等待百炼回复
        RESPONDING,     // 百炼正在回复
        CLOSED          // 连接已关闭
    }

    public SessionState() {
        this(Status.CONNECTING, null);
    }

    public SessionState(Status status, String turnId) {
        this.status = status;
        this.turnId = turnId;
    }

    public Status getStatus() {
        return status;
    }

    public String getTurnId() {
        return turnId;
    }

    public SessionState withStatus(Status status) {
        return new SessionState(status, this.turnId);
    }

    public SessionState withTurnId(String turnId) {
        return new SessionState(this.status, turnId);
    }

    public boolean isActive() {
        return status != Status.CLOSED;
    }

    public boolean isReady() {
        return status == Status.READY || status == Status.SPEAKING
                || status == Status.PROCESSING || status == Status.RESPONDING;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SessionState that)) return false;
        return status == that.status && Objects.equals(turnId, that.turnId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, turnId);
    }

    @Override
    public String toString() {
        return "SessionState{status=" + status + ", turnId='" + turnId + "'}";
    }
}

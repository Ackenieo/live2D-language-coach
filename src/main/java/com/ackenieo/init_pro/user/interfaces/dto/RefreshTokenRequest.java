package com.ackenieo.init_pro.user.interfaces.dto;

/**
 * 刷新Token请求
 */
public class RefreshTokenRequest {
    private String refreshToken;

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}

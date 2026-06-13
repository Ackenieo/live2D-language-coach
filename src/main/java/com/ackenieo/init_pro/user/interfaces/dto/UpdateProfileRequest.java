package com.ackenieo.init_pro.user.interfaces.dto;

/**
 * 更新用户资料请求
 */
public class UpdateProfileRequest {
    private String nickname;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}

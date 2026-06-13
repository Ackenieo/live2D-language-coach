package com.ackenieo.init_pro.user.interfaces.dto;

/**
 * 头像上传响应
 */
public class AvatarUploadResponse {
    private String avatarUrl;

    public AvatarUploadResponse() {
    }

    public AvatarUploadResponse(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}

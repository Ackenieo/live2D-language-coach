package com.ackenieo.init_pro.conversation.interfaces.dto;

/**
 * 排行榜项响应
 */
public class LeaderboardItemResponse {
    private int rank;
    private String userId;
    private String nickname;
    private String avatarUrl;
    private String avgGrade;
    private long sessionCount;

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getAvgGrade() {
        return avgGrade;
    }

    public void setAvgGrade(String avgGrade) {
        this.avgGrade = avgGrade;
    }

    public long getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(long sessionCount) {
        this.sessionCount = sessionCount;
    }
}

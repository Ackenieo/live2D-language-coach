package com.ackenieo.init_pro.conversation.interfaces.dto;

import java.util.List;

/**
 * 排行榜响应
 */
public class LeaderboardResponse {
    private List<LeaderboardItemResponse> records;
    private LeaderboardItemResponse myRank;

    public List<LeaderboardItemResponse> getRecords() {
        return records;
    }

    public void setRecords(List<LeaderboardItemResponse> records) {
        this.records = records;
    }

    public LeaderboardItemResponse getMyRank() {
        return myRank;
    }

    public void setMyRank(LeaderboardItemResponse myRank) {
        this.myRank = myRank;
    }
}

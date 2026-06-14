package com.ackenieo.init_pro.conversation.interfaces.dto;

import java.util.List;

/**
 * 对话历史分页响应
 */
public class ChatHistoryResponse {
    private List<ChatHistoryItemResponse> records;
    private long total;
    private int page;
    private int pageSize;

    public List<ChatHistoryItemResponse> getRecords() {
        return records;
    }

    public void setRecords(List<ChatHistoryItemResponse> records) {
        this.records = records;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}

package com.ackenieo.init_pro.application.query;

/**
 * 查询基类
 */
public abstract class BaseQuery {
    private final String queryId;

    protected BaseQuery() {
        this.queryId = java.util.UUID.randomUUID().toString();
    }

    public String getQueryId() {
        return queryId;
    }
}

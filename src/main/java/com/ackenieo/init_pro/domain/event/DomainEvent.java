package com.ackenieo.init_pro.domain.event;

import java.time.LocalDateTime;

/**
 * 领域事件基类
 */
public abstract class DomainEvent {
    private final String eventId;
    private final String aggregateId;
    private final LocalDateTime occurredOn;
    private final int eventVersion;

    protected DomainEvent(String aggregateId) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.aggregateId = aggregateId;
        this.occurredOn = LocalDateTime.now();
        this.eventVersion = 1;
    }

    public String getEventId() {
        return eventId;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    public int getEventVersion() {
        return eventVersion;
    }
}

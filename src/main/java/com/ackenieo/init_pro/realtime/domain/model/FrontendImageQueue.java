package com.ackenieo.init_pro.realtime.domain.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class FrontendImageQueue {
    public static final int DEFAULT_CAPACITY = 3;

    private final int capacity;
    private final Deque<FrontendImage> images = new ArrayDeque<>();

    public FrontendImageQueue() {
        this(DEFAULT_CAPACITY);
    }

    public FrontendImageQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    public synchronized FrontendImage enqueueAndTail(String base64Image, String prompt) {
        images.addLast(new FrontendImage(base64Image, prompt));
        while (images.size() > capacity) {
            images.removeFirst();
        }
        return images.getLast();
    }

    public synchronized FrontendImage tail() {
        return images.peekLast();
    }

    public synchronized int size() {
        return images.size();
    }

    public synchronized List<FrontendImage> snapshot() {
        return new ArrayList<>(images);
    }
}

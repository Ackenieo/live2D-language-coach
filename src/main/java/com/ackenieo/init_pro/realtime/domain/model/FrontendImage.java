package com.ackenieo.init_pro.realtime.domain.model;

public record FrontendImage(String base64Image, String prompt) {
    public FrontendImage {
        if (base64Image == null || base64Image.isBlank()) {
            throw new IllegalArgumentException("base64Image must not be blank");
        }
        prompt = prompt == null ? "" : prompt;
    }
}

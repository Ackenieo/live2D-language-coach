package com.ackenieo.init_pro.realtime.infrastructure.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BailianRealtimeClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void buildsImageAppendMessageWithoutPromptText() throws Exception {
        String message = BailianRealtimeClient.buildImageAppendMessage("base64-image");

        JsonNode json = objectMapper.readTree(message);
        assertThat(json.get("type").asText()).isEqualTo("input_image_buffer.append");
        assertThat(json.get("image").asText()).isEqualTo("base64-image");
        assertThat(json.has("prompt")).isFalse();
        assertThat(json.size()).isEqualTo(2);
    }
}

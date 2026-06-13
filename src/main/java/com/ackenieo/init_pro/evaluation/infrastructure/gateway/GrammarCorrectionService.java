package com.ackenieo.init_pro.evaluation.infrastructure.gateway;

import com.ackenieo.init_pro.evaluation.domain.gateway.GrammarCorrector;
import com.ackenieo.init_pro.evaluation.infrastructure.config.EvaluationConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 语法纠错服务
 */
@Service
public class GrammarCorrectionService implements GrammarCorrector {
    private static final Logger log = LoggerFactory.getLogger(GrammarCorrectionService.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String correctionModel;
    private final boolean enabled;

    public GrammarCorrectionService(EvaluationConfig config) {
        this.objectMapper = new ObjectMapper();
        this.enabled = config.isDoubaoEnabled();
        this.apiKey = config.getDoubaoApiKey();
        this.baseUrl = config.getDoubaoBaseUrl();
        this.correctionModel = config.getDoubaoCorrectionModel();

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getDoubaoTimeoutMillis()))
                .build();

        if (this.enabled && this.apiKey != null && !this.apiKey.isEmpty()) {
            log.info("语法纠错服务已启用，模型: {}", this.correctionModel);
        } else {
            log.info("语法纠错服务未启用或API Key未配置");
        }
    }

    @Override
    public String correct(String userText) {
        if (!enabled || userText == null || userText.trim().isEmpty()) {
            return userText;
        }

        try {
            String prompt = "You are an English grammar correction assistant. Check the following English text for grammar errors:\n" +
                    "1. Only correct obvious grammar mistakes (verb tense, articles, subject-verb agreement, etc.)\n" +
                    "2. Do not change the speaker's style or expression\n" +
                    "3. Do not add or remove content\n" +
                    "4. If there are no errors, output exactly one word: \u65e0\n" +
                    "5. If there are errors, output only the corrected text with no explanation\n\n" +
                    "Input text: " + userText;

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", correctionModel);

            ArrayNode messages = requestBody.putArray("messages");
            ObjectNode userMessage = messages.addObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode respJson = objectMapper.readTree(response.body());
                JsonNode choices = respJson.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode messageNode = choices.get(0).get("message");
                    if (messageNode != null && messageNode.has("content")) {
                        String corrected = messageNode.get("content").asText().trim();
                        log.info("语法纠正 - 原文: {}, 纠正后: {}", userText, corrected);
                        if (userText.trim().equals(corrected)) {
                            return "\u65e0";
                        }
                        return corrected;
                    }
                }
            } else {
                log.error("语法纠正API调用失败，状态码: {}, 响应: {}", response.statusCode(), response.body());
            }

            return "\u65e0";
        } catch (Exception e) {
            log.error("语法纠正失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}

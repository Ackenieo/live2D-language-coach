package com.ackenieo.init_pro.conversation.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提示词模板管理服务
 */
@Service
public class PromptTemplateService {

    private static final Logger log = LoggerFactory.getLogger(PromptTemplateService.class);
    private static final Pattern TEMPLATE_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_-]+");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([a-zA-Z0-9_-]+)}");

    private static final String DEFAULT_SYSTEM_PROMPT =
            "You are an English speaking coach. Help the user practice English conversation. " +
            "Correct their grammar and pronunciation when appropriate. " +
            "Keep your responses concise and encourage the user to speak more. " +
            "Use simple, natural English that is appropriate for the user's level.";

    private static final Map<String, String> DEFAULT_VARIABLES = Map.ofEntries(
            Map.entry("difficulty", "medium"),
            Map.entry("accent", "US"),
            Map.entry("destination", "your destination"),
            Map.entry("flight_status", "scheduled"),
            Map.entry("meeting_topic", "project planning"),
            Map.entry("symptom", "common symptoms"),
            Map.entry("guest_name", "the guest"),
            Map.entry("room_type", "standard"),
            Map.entry("cuisine", "local"),
            Map.entry("preference", "no special dietary preference"),
            Map.entry("item_category", "everyday"),
            Map.entry("budget", "a reasonable budget"),
            Map.entry("input_text", "")
    );

    private static final String VISION_ENABLED_INSTRUCTION =
            "Image input may be available in the current turn. Treat it as what you can see, " +
            "and answer questions about the image only when it is relevant.";

    private static final String VISION_DISABLED_INSTRUCTION =
            "If the user asks what you can see but no image is available, say \"Excuse me?\" and ask them to show it again.";

    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    public String getSystemPrompt() {
        return getSystemPrompt("default", Map.of());
    }

    public String getSystemPrompt(String templateName, Map<String, String> variables) {
        String template = loadTemplate(templateName);
        return render(template, mergeVariables(variables));
    }

    public String getRealtimeSystemPrompt(String scene, String difficulty, String accent, boolean visionEnabled) {
        Map<String, String> variables = new HashMap<>();
        variables.put("difficulty", difficulty);
        variables.put("accent", accent);
        String prompt = getSystemPrompt(scene, variables);
        return prompt + "\n\n" + (visionEnabled ? VISION_ENABLED_INSTRUCTION : VISION_DISABLED_INSTRUCTION);
    }

    public String getGrammarCorrectionPrompt(String userText) {
        return getSystemPrompt("grammar-correction", Map.of("input_text", userText == null ? "" : userText));
    }

    public void registerTemplate(String name, String template) {
        String templateName = normalizeTemplateName(name);
        templateCache.put(templateName, template);
        log.info("已注册提示词模板: {}", templateName);
    }

    private String loadTemplate(String templateName) {
        templateName = normalizeTemplateName(templateName);
        if (templateCache.containsKey(templateName)) {
            return templateCache.get(templateName);
        }

        String path = "prompts/" + templateName + ".txt";
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (resource.exists()) {
                String content = resource.getContentAsString(StandardCharsets.UTF_8);
                templateCache.put(templateName, content);
                log.info("已加载提示词模板: {}", path);
                return content;
            }
        } catch (IOException e) {
            log.warn("加载提示词模板失败: {}", path, e);
        }

        if (!"default".equals(templateName)) {
            log.info("提示词模板 {} 不存在，使用 default", templateName);
            return loadTemplate("default");
        }

        log.info("默认提示词模板不存在，使用内置提示词");
        return DEFAULT_SYSTEM_PROMPT;
    }

    private String render(String template, Map<String, String> variables) {
        String result = template;
        for (var entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return replaceUnresolvedPlaceholders(result);
    }

    private Map<String, String> mergeVariables(Map<String, String> variables) {
        Map<String, String> merged = new HashMap<>(DEFAULT_VARIABLES);
        if (variables != null) {
            variables.forEach((key, value) -> {
                if (key != null && value != null && !value.isBlank()) {
                    merged.put(key, value);
                }
            });
        }
        return merged;
    }

    private String replaceUnresolvedPlaceholders(String result) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = DEFAULT_VARIABLES.getOrDefault(key, "the current topic");
            log.warn("提示词变量 {} 未提供，使用默认值: {}", key, value);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String normalizeTemplateName(String templateName) {
        if (templateName == null || templateName.isBlank()) {
            return "default";
        }
        String normalized = templateName.trim().toLowerCase(Locale.ROOT);
        if (!TEMPLATE_NAME_PATTERN.matcher(normalized).matches()) {
            log.warn("提示词模板名称非法: {}，使用 default", templateName);
            return "default";
        }
        return normalized;
    }
}

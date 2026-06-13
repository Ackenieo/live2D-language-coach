package com.ackenieo.init_pro.conversation.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 提示词模板管理服务
 */
@Service
public class PromptTemplateService {

    private static final Logger log = LoggerFactory.getLogger(PromptTemplateService.class);

    private static final String DEFAULT_SYSTEM_PROMPT =
            "You are an English speaking coach. Help the user practice English conversation. " +
            "Correct their grammar and pronunciation when appropriate. " +
            "Keep your responses concise and encourage the user to speak more. " +
            "Use simple, natural English that is appropriate for the user's level.";

    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    public PromptTemplateService() {
        templateCache.put("default", DEFAULT_SYSTEM_PROMPT);
    }

    public String getSystemPrompt() {
        return getSystemPrompt("default", Map.of());
    }

    public String getSystemPrompt(String templateName, Map<String, String> variables) {
        String template = loadTemplate(templateName);
        return render(template, variables);
    }

    public void registerTemplate(String name, String template) {
        templateCache.put(name, template);
        log.info("已注册提示词模板: {}", name);
    }

    private String loadTemplate(String templateName) {
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

        log.info("提示词模板 {} 不存在，使用 default", templateName);
        return templateCache.getOrDefault("default", DEFAULT_SYSTEM_PROMPT);
    }

    private String render(String template, Map<String, String> variables) {
        String result = template;
        for (var entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}

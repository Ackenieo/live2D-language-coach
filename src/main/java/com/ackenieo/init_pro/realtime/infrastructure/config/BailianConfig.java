package com.ackenieo.init_pro.realtime.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 百炼 Realtime API 配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "bailian")
public class BailianConfig {
    private String apiKey;
    private String wsUrl;
    private String voice;
}

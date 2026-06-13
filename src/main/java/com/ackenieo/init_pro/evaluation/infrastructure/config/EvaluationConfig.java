package com.ackenieo.init_pro.evaluation.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 评分相关配置
 * 集中管理腾讯智聆 + 豆包的配置
 */
@Configuration
public class EvaluationConfig {

    @Value("${tencent.soe.enabled:false}")
    private boolean tencentSoeEnabled;

    @Value("${tencent.soe.app-id:}")
    private String tencentAppId;

    @Value("${tencent.soe.secret-id:}")
    private String tencentSecretId;

    @Value("${tencent.soe.secret-key:}")
    private String tencentSecretKey;

    @Value("${doubao.enabled:false}")
    private boolean doubaoEnabled;

    @Value("${doubao.api-key:}")
    private String doubaoApiKey;

    @Value("${doubao.base-url:https://ark.cn-beijing.volces.com/api/v3}")
    private String doubaoBaseUrl;

    @Value("${doubao.correction-model:doubao-seed-1-6}")
    private String doubaoCorrectionModel;

    @Value("${doubao.timeout-millis:30000}")
    private long doubaoTimeoutMillis;

    public boolean isTencentSoeEnabled() { return tencentSoeEnabled; }
    public String getTencentAppId() { return tencentAppId; }
    public String getTencentSecretId() { return tencentSecretId; }
    public String getTencentSecretKey() { return tencentSecretKey; }
    public boolean isDoubaoEnabled() { return doubaoEnabled; }
    public String getDoubaoApiKey() { return doubaoApiKey; }
    public String getDoubaoBaseUrl() { return doubaoBaseUrl; }
    public String getDoubaoCorrectionModel() { return doubaoCorrectionModel; }
    public long getDoubaoTimeoutMillis() { return doubaoTimeoutMillis; }
}

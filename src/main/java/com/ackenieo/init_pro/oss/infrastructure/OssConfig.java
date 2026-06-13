package com.ackenieo.init_pro.oss.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OSS 配置
 */
@ConfigurationProperties(prefix = "oss")
public class OssConfig {
    private boolean enabled;
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }
}

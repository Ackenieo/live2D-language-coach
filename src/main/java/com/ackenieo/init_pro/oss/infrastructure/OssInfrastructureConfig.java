package com.ackenieo.init_pro.oss.infrastructure;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OSS 基础设施配置
 * 临时联调说明：当前 accessKeyId/accessKeySecret 为写死值，仅用于排查运行时配置覆盖问题。
 * 提交前必须恢复为从 OssConfig 读取。
 */
@Configuration
@EnableConfigurationProperties(OssConfig.class)
public class OssInfrastructureConfig {
    private static final Logger log = LoggerFactory.getLogger(OssInfrastructureConfig.class);

    private static final String DEBUG_ACCESS_KEY_ID = "LTAI5t5hUsaXhDmRHDEdtuVk";
    private static final String DEBUG_ACCESS_KEY_SECRET = "s37vZ8tywq5FILw6c30AgBXvTv1vhl";

    @Bean(destroyMethod = "shutdown")
    public OSS oss(OssConfig ossConfig) {
        log.warn("OSS DEBUG MODE enabled: forcing hardcoded accessKeyId={}, endpoint={}, bucket={}",
                DEBUG_ACCESS_KEY_ID,
                ossConfig.getEndpoint(),
                ossConfig.getBucketName());
        return new OSSClientBuilder().build(
                ossConfig.getEndpoint(),
                DEBUG_ACCESS_KEY_ID,
                DEBUG_ACCESS_KEY_SECRET
        );
    }
}

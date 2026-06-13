package com.ackenieo.init_pro.oss.infrastructure;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OSS 基础设施配置
 */
@Configuration
@EnableConfigurationProperties(OssConfig.class)
public class OssInfrastructureConfig {

    @Bean(destroyMethod = "shutdown")
    public OSS oss(OssConfig ossConfig) {
        return new OSSClientBuilder().build(
                ossConfig.getEndpoint(),
                ossConfig.getAccessKeyId(),
                ossConfig.getAccessKeySecret()
        );
    }
}

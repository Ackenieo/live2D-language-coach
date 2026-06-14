package com.ackenieo.init_pro.shared.infrastructure.ratelimit;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.SentinelWebInterceptor;
import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.config.SentinelWebMvcConfig;
import com.alibaba.csp.sentinel.adapter.web.common.UrlCleaner;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SentinelRateLimitConfig {

    @Bean
    public UrlCleaner sentinelResourceNameCleaner() {
        return new SentinelResourceNameCleaner();
    }

    @Bean
    public BlockExceptionHandler sentinelRateLimitBlockHandler(ObjectMapper objectMapper) {
        return new SentinelRateLimitBlockHandler(objectMapper);
    }

    @Bean
    public SentinelWebMvcConfig sentinelWebMvcConfig(UrlCleaner sentinelResourceNameCleaner,
                                                    BlockExceptionHandler sentinelRateLimitBlockHandler) {
        SentinelWebMvcConfig config = new SentinelWebMvcConfig();
        config.setUrlCleaner(sentinelResourceNameCleaner);
        config.setBlockExceptionHandler(sentinelRateLimitBlockHandler);
        config.setHttpMethodSpecify(true);
        config.setWebContextUnify(true);
        config.setContextPathSpecify(false);
        return config;
    }

    @Bean
    public SentinelWebInterceptor sentinelWebInterceptor(SentinelWebMvcConfig sentinelWebMvcConfig) {
        return new HttpMethodSentinelWebInterceptor(sentinelWebMvcConfig);
    }
}

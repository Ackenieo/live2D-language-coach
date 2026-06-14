package com.ackenieo.init_pro.shared.infrastructure;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.SentinelWebInterceptor;
import com.ackenieo.init_pro.user.infrastructure.security.HttpAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final HttpAuthInterceptor httpAuthInterceptor;
    private final SentinelWebInterceptor sentinelWebInterceptor;

    public WebMvcConfig(HttpAuthInterceptor httpAuthInterceptor,
                        SentinelWebInterceptor sentinelWebInterceptor) {
        this.httpAuthInterceptor = httpAuthInterceptor;
        this.sentinelWebInterceptor = sentinelWebInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sentinelWebInterceptor)
                .order(Ordered.HIGHEST_PRECEDENCE)
                .addPathPatterns("/api/**");

        registry.addInterceptor(httpAuthInterceptor)
                .order(Ordered.HIGHEST_PRECEDENCE + 1)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/test/**");
    }
}

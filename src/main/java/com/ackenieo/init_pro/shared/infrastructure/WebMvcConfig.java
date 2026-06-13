package com.ackenieo.init_pro.shared.infrastructure;

import com.ackenieo.init_pro.user.infrastructure.security.HttpAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final HttpAuthInterceptor httpAuthInterceptor;

    public WebMvcConfig(HttpAuthInterceptor httpAuthInterceptor) {
        this.httpAuthInterceptor = httpAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(httpAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/test/**");
    }
}

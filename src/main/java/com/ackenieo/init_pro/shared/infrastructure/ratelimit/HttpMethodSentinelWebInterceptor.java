package com.ackenieo.init_pro.shared.infrastructure.ratelimit;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.SentinelWebInterceptor;
import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.config.SentinelWebMvcConfig;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;

public class HttpMethodSentinelWebInterceptor extends SentinelWebInterceptor {

    public HttpMethodSentinelWebInterceptor(SentinelWebMvcConfig config) {
        super(config);
    }

    @Override
    protected String getResourceName(HttpServletRequest request) {
        String resourceName = super.getResourceName(request);
        if (resourceName == null || resourceName.isBlank()) {
            return resourceName;
        }
        return request.getMethod().toUpperCase(Locale.ROOT) + ":" + resourceName;
    }
}

package com.ackenieo.init_pro.shared.infrastructure.ratelimit;

import com.alibaba.csp.sentinel.adapter.web.common.UrlCleaner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SentinelResourceNameCleaner implements UrlCleaner {
    private static final Pattern CHAT_SESSION_RESOURCE =
            Pattern.compile("^/api/chat/sessions/[^/]+/(history|summary)$");
    private static final Pattern CHAT_REPORT_RESOURCE =
            Pattern.compile("^/api/chat/report/[^/]+$");

    @Override
    public String clean(String originUrl) {
        if (originUrl == null || originUrl.isBlank()) {
            return originUrl;
        }

        String resourceName = removeQueryString(originUrl);
        String methodPrefix = "";
        String path = resourceName;
        int methodSeparator = resourceName.indexOf(":/");
        if (methodSeparator > 0) {
            methodPrefix = resourceName.substring(0, methodSeparator + 1);
            path = resourceName.substring(methodSeparator + 1);
        }

        Matcher chatSessionMatcher = CHAT_SESSION_RESOURCE.matcher(path);
        if (chatSessionMatcher.matches()) {
            return methodPrefix + "/api/chat/sessions/{sessionId}/" + chatSessionMatcher.group(1);
        }

        if (CHAT_REPORT_RESOURCE.matcher(path).matches()) {
            return methodPrefix + "/api/chat/report/{sessionId}";
        }

        return methodPrefix + path;
    }

    private String removeQueryString(String value) {
        int queryStart = value.indexOf('?');
        if (queryStart < 0) {
            return value;
        }
        return value.substring(0, queryStart);
    }
}

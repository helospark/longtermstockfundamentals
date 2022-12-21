package com.helospark.financialdata.management.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CacheInterceptor implements HandlerInterceptor {
    private static final int ONE_DAY = 60 * 60 * 24;

    @Value("${application.cache.enabled}")
    private boolean cachingEnabled;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (cachingEnabled) {
            if (response.getStatus() >= 200 && response.getStatus() < 300 && request.getRequestURI() != null && request.getRequestURI().length() < 100) {
                if (request.getRequestURI().matches("/.*?/financials/.*")) {
                    response.addHeader("Cache-Control", "max-age=" + ONE_DAY + ", private, no-transform");
                }
            }
        }
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

}

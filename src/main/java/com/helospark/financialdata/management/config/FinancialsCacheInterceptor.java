package com.helospark.financialdata.management.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class FinancialsCacheInterceptor implements HandlerInterceptor {
    @Value("${application.financials.cache.enabled}")
    private boolean cachingEnabled;
    @Value("${application.financials.cache.cache-period:0}")
    private int cachePeriod;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (cachingEnabled) {
            if (response.getStatus() >= 200 && response.getStatus() < 300 && request.getRequestURI() != null && request.getRequestURI().length() < 100) {
                if (request.getRequestURI().matches("/.*?/financials/.*")) {
                    response.addHeader("Cache-Control", "max-age=" + cachePeriod + ", private, no-transform");
                }
            }
        }
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

}

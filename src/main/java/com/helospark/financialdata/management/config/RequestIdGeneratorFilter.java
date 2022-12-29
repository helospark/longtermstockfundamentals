package com.helospark.financialdata.management.config;

import java.io.IOException;
import java.util.UUID;

import org.jboss.logging.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(0)
public class RequestIdGeneratorFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String requestId = UUID.randomUUID().toString();

        MDC.put("requestId", requestId);

        if (response instanceof HttpServletResponse) {
            ((HttpServletResponse) response).addHeader("request-id", requestId);
            ((HttpServletResponse) response).addHeader("request-id", requestId);
            MDC.put("clientIp", ((HttpServletRequest) request).getRemoteAddr());
        }

        chain.doFilter(request, response);
    }

}

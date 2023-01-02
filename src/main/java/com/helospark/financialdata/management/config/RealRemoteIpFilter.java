package com.helospark.financialdata.management.config;

import java.io.IOException;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

@Component
@Order(0)
public class RealRemoteIpFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        var httpRequest = ((HttpServletRequest) request);

        String realIp = httpRequest.getHeader("X-Real-IP");
        if (realIp == null) {
            realIp = httpRequest.getRemoteAddr();
        }

        var requestWrapper = new RealIpRequestWrapper(httpRequest, realIp);

        chain.doFilter(requestWrapper, response);
    }

}

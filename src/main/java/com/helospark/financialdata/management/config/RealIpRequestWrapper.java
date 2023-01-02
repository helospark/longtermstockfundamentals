package com.helospark.financialdata.management.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class RealIpRequestWrapper extends HttpServletRequestWrapper {
    private String realIp;

    public RealIpRequestWrapper(HttpServletRequest request, String realIp) {
        super(request);
    }

    @Override
    public String getRemoteAddr() {
        return realIp;
    }

}

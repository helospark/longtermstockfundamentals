package com.helospark.financialdata.management.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfiguration implements WebMvcConfigurer {
    @Autowired
    private RateLimitingInterceptor tooManyRequestBlockingInterceptor;
    @Autowired
    private FinancialsCacheInterceptor cacheInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tooManyRequestBlockingInterceptor);
        registry.addInterceptor(cacheInterceptor);
    }

}
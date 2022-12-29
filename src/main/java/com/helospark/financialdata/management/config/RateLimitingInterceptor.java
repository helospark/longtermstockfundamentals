package com.helospark.financialdata.management.config;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.helospark.financialdata.management.config.ratelimit.RateLimit;
import com.helospark.financialdata.management.config.ratelimit.RateLimitConfig;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitingInterceptor implements HandlerInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitingInterceptor.class);
    Cache<Method, RateLimitConfig> methodToRateLimitConfigMap = Caffeine.newBuilder()
            .maximumSize(1000)
            .build();
    Cache<String, Bucket> keyToBucket;

    @Value("${ratelimit.enabled:true}")
    private boolean rateLimitEnabled;

    public RateLimitingInterceptor(@Value("${ratelimit.cache-size:10000}") int rateLimitCacheSize) {
        keyToBucket = Caffeine.newBuilder()
                .maximumSize(rateLimitCacheSize)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (rateLimitEnabled) {
            try {
                if (handler instanceof HandlerMethod) {
                    Method handlerMethod = ((HandlerMethod) handler).getMethod();

                    RateLimitConfig rateLimitConfig = methodToRateLimitConfigMap.get(handlerMethod, (method) -> extractRateLimitConfig(method));

                    if (rateLimitConfig != null && rateLimitConfig.enabled) {
                        String remoteIp = request.getRemoteAddr();

                        String key = remoteIp + " " + handlerMethod.toString();

                        Bucket bucket = keyToBucket.get(key, key2 -> createBucket(rateLimitConfig));

                        if (bucket != null) {
                            if (!bucket.tryConsume(1)) {
                                response.getWriter().write("{\"errorMessage\": \"Too many requests\"}");
                                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());

                                return false;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error while doing rate limiting", e);
            }
        }
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    private Bucket createBucket(RateLimitConfig rateLimitConfig) {
        int perMinuteLimit = rateLimitConfig.rateLimitPerMinute;
        return Bucket.builder()
                .addLimit(Bandwidth.classic(perMinuteLimit, Refill.intervally(perMinuteLimit, Duration.ofMinutes(1))))
                .build();
    }

    private RateLimitConfig extractRateLimitConfig(Method handlerMethod) {
        RateLimit rateLimit = handlerMethod.getDeclaredAnnotation(RateLimit.class);

        if (rateLimit == null || rateLimit.requestPerMinute() <= 0) {
            return new RateLimitConfig(false, 0);
        } else {
            return new RateLimitConfig(true, rateLimit.requestPerMinute());
        }
    }

}

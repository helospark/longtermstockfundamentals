package com.helospark.financialdata.management.config.ratelimit;

public class RateLimitConfig {
    public boolean enabled;
    public int rateLimitPerMinute;

    public RateLimitConfig(boolean enabled, int rateLimitPerMinute) {
        this.enabled = enabled;
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

}

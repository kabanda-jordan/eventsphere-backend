package com.eventsphere.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Getter
@Configuration
public class CacheConfig {

    @Value("${otp.expiry-minutes:5}")
    private long otpExpiryMinutes;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("captchaCache");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES));
        return cacheManager;
    }
}

package com.payment.orchestration.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for caching using Caffeine.
 * Used for idempotency record caching and provider response caching.
 */
@Configuration
public class CacheConfig {

    /**
     * Configure Caffeine cache manager
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    /**
     * Build Caffeine cache with configuration
     */
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(10000)                    // Max entries
                .expireAfterWrite(24, TimeUnit.HOURS)  // TTL for idempotency
                .recordStats();                         // Enable statistics
    }

    /**
     * Create specific cache for idempotency records
     */
    @Bean
    public com.github.benmanes.caffeine.cache.Cache<String, Object> idempotencyCache() {
        return Caffeine.newBuilder()
                .maximumSize(50000)                    // Higher limit for idempotency
                .expireAfterWrite(24, TimeUnit.HOURS)  // 24 hour TTL
                .recordStats()
                .build();
    }

    /**
     * Create cache for provider health status
     */
    @Bean
    public com.github.benmanes.caffeine.cache.Cache<String, Boolean> providerHealthCache() {
        return Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(30, TimeUnit.SECONDS)  // Short TTL for health checks
                .build();
    }
}

package com.droid.bss.infrastructure.config;

import com.droid.bss.infrastructure.resilience.ResilienceRateLimitingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for interceptors and filters
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ResilienceRateLimitingInterceptor rateLimitingInterceptor;

    public WebMvcConfig(ResilienceRateLimitingInterceptor rateLimitingInterceptor) {
        this.rateLimitingInterceptor = rateLimitingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Add rate limiting interceptor
        registry.addInterceptor(rateLimitingInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/public/**",
                        "/actuator/**",
                        "/health",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );
    }
}

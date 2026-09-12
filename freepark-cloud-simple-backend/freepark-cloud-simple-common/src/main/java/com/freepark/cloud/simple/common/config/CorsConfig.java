package com.freepark.cloud.simple.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局跨域：放行前端（用户端 / 管理端）直连 API。
 * <p>
 * JWT 放在 Authorization 头，不依赖 Cookie，因此不强制 allowCredentials。
 * 允许来源可通过 {@code freepark.cors.allowed-origin-patterns} 配置，默认 {@code *}。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${freepark.cors.allowed-origin-patterns:*}")
    private String allowedOriginPatterns;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] patterns = splitPatterns(allowedOriginPatterns);
        registry.addMapping("/api/**")
                .allowedOriginPatterns(patterns)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }

    private static String[] splitPatterns(String raw) {
        if (raw == null || raw.isBlank()) {
            return new String[] {"*"};
        }
        return java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }
}

package com.freepark.cloud.simple.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freepark.cloud.simple.common.i18n.MessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * 认证配置：为 /api/** 接口注册 JWT 拦截器，默认放行登录接口。
 */
@Configuration
public class WebAuthConfig implements WebMvcConfigurer {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final MessageService messageService;

    public WebAuthConfig(JwtUtil jwtUtil, ObjectMapper objectMapper, MessageService messageService) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.messageService = messageService;
    }

    @Value("${freepark.auth.exclude-paths:}")
    private String excludePaths;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtAuthInterceptor(jwtUtil, objectMapper, messageService, excludeList()))
                .addPathPatterns("/api/**");
    }

    private List<String> excludeList() {
        List<String> paths = new ArrayList<>();
        paths.add("/api/user/login");
        // 边缘节点算费接口：边缘按车牌查询欠费金额，无云端账号体系，需免鉴权开放
        paths.add("/api/edge/arrears-quote");
        // C 端公开查询（用户端网页按车牌查费）：免鉴权，仅暴露 /api/public 命名空间
        paths.add("/api/public/**");
        if (excludePaths != null && !excludePaths.isBlank()) {
            for (String item : excludePaths.split(",")) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) {
                    paths.add(trimmed);
                }
            }
        }
        return paths;
    }
}

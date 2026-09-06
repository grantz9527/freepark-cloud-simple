package com.freepark.cloud.simple.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.i18n.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 认证拦截器：拦截受保护接口，校验 Authorization: Bearer token。
 */
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final MessageService messageService;
    /** 无需登录即可访问的路径白名单 */
    private final List<String> excludePaths;

    public JwtAuthInterceptor(JwtUtil jwtUtil,
                              ObjectMapper objectMapper,
                              MessageService messageService,
                              List<String> excludePaths) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.messageService = messageService;
        this.excludePaths = excludePaths;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || excludePaths.contains(path)) {
            return true;
        }

        String token = resolveToken(request);
        try {
            if (token == null) {
                throw new IllegalArgumentException("missing token");
            }
            AuthContext.set(jwtUtil.parseUsername(token));
            return true;
        } catch (Exception e) {
            AuthContext.clear();
            writeUnauthorized(response);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        AuthContext.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String message = messageService.get(MessageKeys.AUTH_UNAUTHORIZED);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResult.fail(401, message)));
    }
}

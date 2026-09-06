package com.freepark.cloud.simple.user.dto;

/**
 * 登录响应。
 */
public record LoginResponse(String token, String username, String nickname, String role) {
}

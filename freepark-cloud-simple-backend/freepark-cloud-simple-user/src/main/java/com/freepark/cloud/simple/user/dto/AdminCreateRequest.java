package com.freepark.cloud.simple.user.dto;

/**
 * 新增管理员请求。
 */
public record AdminCreateRequest(String username, String password, String nickname) {
}

package com.freepark.cloud.simple.user.dto;

import java.time.LocalDateTime;

/**
 * 管理员列表 / 当前用户信息项。
 */
public record UserItem(Long id, String username, String nickname, String role,
                       Integer status, LocalDateTime createdAt) {
}

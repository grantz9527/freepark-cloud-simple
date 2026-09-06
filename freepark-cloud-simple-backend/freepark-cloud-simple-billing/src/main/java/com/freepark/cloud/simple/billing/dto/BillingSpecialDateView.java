package com.freepark.cloud.simple.billing.dto;

import java.time.LocalDateTime;

/**
 * 计费特殊时间段视图。
 *
 * <p>库内时间为 UTC 锚点；本视图中的时间均为「系统配置时区」下的本地挂钟时间，
 * 由服务层统一换算，勿直接由实体构造。</p>
 */
public record BillingSpecialDateView(
        Long id,
        String type,
        String name,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

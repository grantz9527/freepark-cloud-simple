package com.freepark.cloud.simple.billing.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 计费周期方案视图（含有序分段列表）。tailMode 取值见 {@code CycleTailMode}。
 */
public record BillingCycleProfileView(
        Long id,
        String name,
        String description,
        String tailMode,
        List<BillingCycleSegmentView> segments,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

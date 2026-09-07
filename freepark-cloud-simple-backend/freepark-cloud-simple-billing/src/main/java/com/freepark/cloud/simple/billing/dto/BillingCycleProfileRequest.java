package com.freepark.cloud.simple.billing.dto;

import java.util.List;

/**
 * 计费周期方案的创建/更新请求（全局共享方案）。
 *
 * @param name        方案名称
 * @param description 方案描述（可选）
 * @param tailMode    尾数计费方式，取值见 {@code CycleTailMode}
 * @param segments    分段列表（按顺序消耗，至少一段）
 */
public record BillingCycleProfileRequest(
        String name,
        String description,
        String tailMode,
        List<BillingCycleSegmentRequest> segments) {
}

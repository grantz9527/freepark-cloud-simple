package com.freepark.cloud.simple.billing.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 车场计费配置（规则模板 × 车场绑定）视图。plateColor 为 null 表示默认（适用其余颜色）；
 * effectiveFrom/effectiveTo 为空表示不限；ruleTitle 为所引用模板的当前标题（实时解析，便于界面展示）。
 */
public record BillingLotBindingView(
        Long id,
        Long lotId,
        String ruleType,
        Long ruleId,
        String ruleTitle,
        String plateColor,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

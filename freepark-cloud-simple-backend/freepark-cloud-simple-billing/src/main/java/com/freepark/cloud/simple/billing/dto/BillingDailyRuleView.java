package com.freepark.cloud.simple.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 每日制计费规则（全局计费模板）视图；slots 为周计划明细（按星期、开始时间升序；
 * 空列表表示全部星期默认全天收费）。
 */
public record BillingDailyRuleView(
        Long id,
        String title,
        String description,
        int freeMinutes,
        int graceMinutes,
        int cycleMinutes,
        BigDecimal unitPriceYuan,
        BigDecimal capPerDayYuan,
        Long cycleProfileId,
        List<BillingDailySlotView> slots,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

package com.freepark.cloud.simple.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 24 小时制计费规则（全局计费模板）视图；slots 为周计划明细
 * （按星期、开始时间升序；空列表表示全部星期默认全天收费）。
 */
public record BillingGeneralRuleView(
        Long id,
        String title,
        String description,
        int freeMinutes,
        int graceMinutes,
        boolean weekendFree,
        boolean holidayFree,
        int cycleMinutes,
        BigDecimal unitPriceYuan,
        BigDecimal capPer24hYuan,
        Long cycleProfileId,
        List<BillingGeneralSlotView> slots,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

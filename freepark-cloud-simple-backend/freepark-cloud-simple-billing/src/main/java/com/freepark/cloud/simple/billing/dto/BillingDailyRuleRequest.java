package com.freepark.cloud.simple.billing.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 每日制计费规则（全局计费模板）的创建/更新请求。
 * <p>
 * 模板本身不绑定车场或车牌颜色；车场按需在「计费配置」中引用（见 BillingLotBindingRequest）。
 * 时长均按分钟；金额单位均为元（最多两位小数）。{@code slots} 为「周计划」明细：
 * 未出现的星期默认全天收费，详见 {@link BillingDailySlotRequest}。
 * </p>
 */
public record BillingDailyRuleRequest(
        String title,
        String description,
        int freeMinutes,
        int graceMinutes,
        int cycleMinutes,
        BigDecimal unitPriceYuan,
        BigDecimal capPerDayYuan,
        Long cycleProfileId,
        List<BillingDailySlotRequest> slots) {
}

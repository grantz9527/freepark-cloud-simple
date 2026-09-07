package com.freepark.cloud.simple.billing.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 24 小时制计费规则（全局计费模板）的创建/更新请求。
 * <p>
 * 模板本身不绑定车场或车牌颜色；车场按需在「计费配置」中引用（见 BillingLotBindingRequest）。
 * 时长均按分钟；金额单位均为元（最多两位小数）。{@code slots} 为「周计划」明细：
 * 未出现的星期默认全天收费；注意「周六日/节假日免费」开关优先于周计划，详见
 * {@link BillingGeneralSlotRequest}。
 * </p>
 */
public record BillingGeneralRuleRequest(
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
        List<BillingGeneralSlotRequest> slots) {
}

package com.freepark.cloud.simple.billing.dto;

/**
 * 每日制规则「周计划」明细视图（同一规则内按星期、开始时间排序）。
 * <p>
 * {@code allDayFree=true} 表示该星期当天整体免费（start/end 为 null）；
 * {@code allDayFree=false} 表示一个收费时段（分钟数，endMinute 可为 1440 = 24:00）；
 * 某星期没有行表示默认「全天收费」。
 * </p>
 */
public record BillingDailySlotView(
        Long id,
        int weekday,
        boolean allDayFree,
        Integer startMinute,
        Integer endMinute) {
}

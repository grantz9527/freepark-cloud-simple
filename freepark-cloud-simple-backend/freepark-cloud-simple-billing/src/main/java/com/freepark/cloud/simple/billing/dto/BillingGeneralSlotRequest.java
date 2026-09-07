package com.freepark.cloud.simple.billing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 24 小时制规则「周计划」中某一行的创建/更新请求。
 * <p>
 * {@code allDayFree=true} 表示该星期「当天免费」，此时忽略 start/end；
 * {@code allDayFree=false} 表示一个收费时段 {@code [startMinute, endMinute)}，
 * 同一天可提交多行以表达多个收费时段；某星期不提交任何行表示默认「全天收费」。
 * 时间为当天 0 点起分钟数（endMinute 可为 1440 表示 24:00）。
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BillingGeneralSlotRequest(
        int weekday,
        boolean allDayFree,
        Integer startMinute,
        Integer endMinute) {
}

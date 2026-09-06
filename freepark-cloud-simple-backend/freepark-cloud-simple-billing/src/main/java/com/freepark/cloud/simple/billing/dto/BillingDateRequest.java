package com.freepark.cloud.simple.billing.dto;

import java.time.LocalDateTime;

/**
 * 新增/更新计费特殊时间段请求。
 *
 * @param type      特殊时段类型：HOLIDAY（节假日）或 MAKEUP_WORKDAY（补班）
 * @param name      名称/说明（可选）
 * @param startTime 开始时间（含），精确到分钟，格式 yyyy-MM-ddTHH:mm:ss
 * @param endTime   结束时间（不含），精确到分钟；整天请选次日 00:00 作为结束
 */
public record BillingDateRequest(
        String type,
        String name,
        LocalDateTime startTime,
        LocalDateTime endTime) {
}

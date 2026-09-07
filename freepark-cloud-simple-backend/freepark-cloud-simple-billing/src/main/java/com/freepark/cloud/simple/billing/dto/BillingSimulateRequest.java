package com.freepark.cloud.simple.billing.dto;

import java.time.LocalDateTime;

/**
 * 模拟算费请求：为一段「入场 → 出场」停车区间按某条计费模板试算费用。
 *
 * @param startTime 入场时间（含），精确到分钟，格式 yyyy-MM-ddTHH:mm:ss
 * @param endTime   出场时间（不含），精确到分钟，须晚于入场时间
 */
public record BillingSimulateRequest(
        LocalDateTime startTime,
        LocalDateTime endTime) {
}

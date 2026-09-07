package com.freepark.cloud.simple.billing.dto;

import java.math.BigDecimal;

/**
 * 计费周期方案中一个分段行的视图。
 * <p>
 * {@code seq} 为该行在方案内的顺序（从 1 起）；{@code repeatCount} 表示计费时该行
 * 需展开的连续档位数（默认 1），展开后的累计时长与档位序列由消费方据此计算。
 * </p>
 */
public record BillingCycleSegmentView(
        Long id,
        int seq,
        int minutes,
        BigDecimal unitPriceYuan,
        int repeatCount) {
}

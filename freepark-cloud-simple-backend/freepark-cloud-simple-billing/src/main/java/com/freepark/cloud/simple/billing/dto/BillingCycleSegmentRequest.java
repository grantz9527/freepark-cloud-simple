package com.freepark.cloud.simple.billing.dto;

import java.math.BigDecimal;

/**
 * 计费周期方案中的一个分段（请求用）。按列表顺序即为消耗顺序（第 1 段起）。
 * <p>
 * {@code repeatCount} 为该行的重复次数，缺省按 1 处理：计费时本行按顺序连续展开为
 * {@code repeatCount} 个相同档位。例如第 1 行「60 分钟 5 元、重复 1 次」+ 第 2 行
 * 「60 分钟 1 元、重复 9 次」，等价于第 1 档 5 元、第 2~10 档各 1 元。
 * </p>
 *
 * @param minutes       本行每档时长（分钟）
 * @param unitPriceYuan 本行每档单价（元）
 * @param repeatCount   本行重复次数（1~99999，可为 null 表示 1）
 */
public record BillingCycleSegmentRequest(
        int minutes,
        BigDecimal unitPriceYuan,
        Integer repeatCount) {
}

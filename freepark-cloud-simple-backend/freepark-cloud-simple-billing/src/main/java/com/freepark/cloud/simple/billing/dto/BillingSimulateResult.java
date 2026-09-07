package com.freepark.cloud.simple.billing.dto;

import java.math.BigDecimal;

/**
 * 模拟算费结果：按某条计费模板对「入场 → 出场」区间试算后的应收金额。
 *
 * @param totalYuan 模拟应收总额（元），0 表示区间内未产生费用
 */
public record BillingSimulateResult(
        BigDecimal totalYuan) {
}

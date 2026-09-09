package com.freepark.cloud.simple.parking.dto;

import java.math.BigDecimal;

/**
 * 流水本次可收款金额预览（收款/下单前置口径）。
 *
 * @param receivableYuan 当前应收口径（元）：已出场取应收快照，在场按「入场 ~ 当前」估算
 * @param paidYuan       该流水累计已支付金额（元）
 * @param pendingYuan    该流水未支付/待支付订单金额（元）合计
 * @param payableYuan    本次可收款金额（元）= 应收 − 已支付 − 未付订单；≤ 0 表示无需再收
 */
public record PayableQuoteView(
        Long sessionId,
        BigDecimal receivableYuan,
        BigDecimal paidYuan,
        BigDecimal pendingYuan,
        BigDecimal payableYuan) {
}

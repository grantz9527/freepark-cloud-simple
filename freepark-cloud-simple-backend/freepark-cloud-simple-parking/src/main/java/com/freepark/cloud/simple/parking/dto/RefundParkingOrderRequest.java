package com.freepark.cloud.simple.parking.dto;

import java.math.BigDecimal;

/**
 * 停车订单退款请求：已支付订单可全部退款或部分退款。
 *
 * @param amountYuan 本次退款金额（元）；缺省或 null 表示按剩余可退金额全额退款
 * @param reason     退款原因（可选，最多 200 字）
 */
public record RefundParkingOrderRequest(BigDecimal amountYuan, String reason) {
}

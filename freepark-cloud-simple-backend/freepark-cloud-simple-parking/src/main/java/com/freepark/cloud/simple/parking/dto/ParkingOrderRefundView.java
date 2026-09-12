package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.ParkingOrderRefund;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 停车订单退款记录视图。
 *
 * @param amountYuan         本次退款金额（元）
 * @param refundedAfterYuan  本单累计已退（含本次，元）
 * @param remainingAfterYuan 本单剩余可退（本次之后，元）
 * @param refundType         PARTIAL 部分退款 / FULL 全部退完
 */
public record ParkingOrderRefundView(
        Long id,
        String refundNo,
        Long orderId,
        String orderNo,
        Long sessionId,
        Long lotId,
        String lotName,
        String plateNumber,
        String plateColor,
        BigDecimal amountYuan,
        BigDecimal refundedAfterYuan,
        BigDecimal remainingAfterYuan,
        String refundType,
        String reason,
        Long operatorId,
        String operatorUsername,
        String operatorNickname,
        LocalDateTime createdAt) {

    public static ParkingOrderRefundView from(ParkingOrderRefund refund) {
        return new ParkingOrderRefundView(
                refund.getId(),
                refund.getRefundNo(),
                refund.getOrderId(),
                refund.getOrderNo(),
                refund.getSessionId(),
                refund.getLotId(),
                refund.getLotName(),
                refund.getPlateNumber(),
                refund.getPlateColor() == null ? null : refund.getPlateColor().name(),
                refund.getAmountYuan(),
                refund.getRefundedAfterYuan(),
                refund.getRemainingAfterYuan(),
                refund.getRefundType() == null ? null : refund.getRefundType().name(),
                refund.getReason(),
                refund.getOperatorId(),
                refund.getOperatorUsername(),
                refund.getOperatorNickname(),
                refund.getCreatedAt());
    }
}

package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.ParkingOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 停车订单视图。
 *
 * @param receivableYuan 下单时应收口径快照（元）：已出场取流水应收快照，在场按下单时点估算
 * @param paidBeforeYuan 下单前该流水累计已支付金额（元）快照
 * @param pendingBeforeYuan 下单前该流水未支付/待支付订单金额（元）合计快照
 * @param amountYuan     本单应付金额（元）：应收 − 已支付 − 未付订单，支付后累加到流水累计已支付
 */
public record ParkingOrderView(
        Long id,
        String orderNo,
        Long sessionId,
        String sessionStatus,
        Long lotId,
        String lotName,
        String plateNumber,
        String plateColor,
        LocalDateTime entryTime,
        BigDecimal receivableYuan,
        BigDecimal paidBeforeYuan,
        BigDecimal pendingBeforeYuan,
        BigDecimal amountYuan,
        String status,
        LocalDateTime payTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ParkingOrderView from(ParkingOrder order) {
        return new ParkingOrderView(
                order.getId(),
                order.getOrderNo(),
                order.getSessionId(),
                order.getSessionStatus() == null ? null : order.getSessionStatus().name(),
                order.getLotId(),
                order.getLotName(),
                order.getPlateNumber(),
                order.getPlateColor() == null ? null : order.getPlateColor().name(),
                order.getEntryTime(),
                order.getReceivableYuan(),
                order.getPaidBeforeYuan(),
                order.getPendingBeforeYuan(),
                order.getAmountYuan(),
                order.getStatus() == null ? null : order.getStatus().name(),
                order.getPayTime(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}

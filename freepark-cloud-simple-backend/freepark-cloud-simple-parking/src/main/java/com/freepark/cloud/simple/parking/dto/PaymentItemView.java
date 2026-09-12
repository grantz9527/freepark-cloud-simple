package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.ParkingOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 一次在线缴款按流水拆分出的单笔明细（对应一笔停车订单）。
 *
 * @param sessionId     关联停车流水 ID
 * @param lotName       车场名称（快照）
 * @param plateColor    车牌颜色枚举名（快照，可为 null）
 * @param sessionStatus 下单时流水状态：OPEN 在场 / CLOSED 已出场
 * @param entryTime     入场时间（快照，序列化为站点本地时间）
 * @param amountYuan    该笔金额（元）
 */
public record PaymentItemView(
        Long sessionId,
        String lotName,
        String plateColor,
        String sessionStatus,
        LocalDateTime entryTime,
        BigDecimal amountYuan) {

    public static PaymentItemView from(ParkingOrder order) {
        return new PaymentItemView(
                order.getSessionId(),
                order.getLotName(),
                order.getPlateColor() == null ? null : order.getPlateColor().name(),
                order.getSessionStatus() == null ? null : order.getSessionStatus().name(),
                order.getEntryTime(),
                order.getAmountYuan());
    }
}

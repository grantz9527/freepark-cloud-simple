package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.PayRecordLot;

import java.math.BigDecimal;

/**
 * 支付记录中单个车场的金额。
 */
public record PayRecordLotView(
        Long lotId,
        String lotName,
        BigDecimal amountYuan) {

    public static PayRecordLotView from(PayRecordLot lot) {
        return new PayRecordLotView(lot.getLotId(), lot.getLotName(), lot.getAmountYuan());
    }
}

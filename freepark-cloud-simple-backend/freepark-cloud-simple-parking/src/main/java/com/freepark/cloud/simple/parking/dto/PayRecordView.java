package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.PayRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付平台流水视图：一笔支付请求或退款请求，附带各车场金额。
 *
 * @param kind      PAY 支付 / REFUND 退款
 * @param platform  WECHAT_PAY / ALIPAY_PAY / CASH
 * @param status    PENDING / SUCCESS / CLOSED
 * @param lots      本请求在各车场的金额，后续可按车场汇总
 */
public record PayRecordView(
        Long id,
        String recordNo,
        String kind,
        String platform,
        String status,
        BigDecimal amountYuan,
        String plateNumber,
        String plateColor,
        String relatedPayNo,
        String relatedOrderNo,
        String relatedRefundNo,
        String transactionId,
        boolean mock,
        String operatorUsername,
        String operatorNickname,
        LocalDateTime successTime,
        LocalDateTime createdAt,
        List<PayRecordLotView> lots) {

    public static PayRecordView from(PayRecord record, List<PayRecordLotView> lots) {
        return new PayRecordView(
                record.getId(),
                record.getRecordNo(),
                record.getKind() == null ? null : record.getKind().name(),
                record.getPlatform() == null ? null : record.getPlatform().name(),
                record.getStatus() == null ? null : record.getStatus().name(),
                record.getAmountYuan(),
                record.getPlateNumber(),
                record.getPlateColor() == null ? null : record.getPlateColor().name(),
                record.getRelatedPayNo(),
                record.getRelatedOrderNo(),
                record.getRelatedRefundNo(),
                record.getTransactionId(),
                record.isMock(),
                record.getOperatorUsername(),
                record.getOperatorNickname(),
                record.getSuccessTime(),
                record.getCreatedAt(),
                lots == null ? List.of() : lots);
    }
}

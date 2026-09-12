package com.freepark.cloud.simple.parking.entity;

/**
 * 支付记录所属平台：线上渠道与管理端现金收款分开记账，便于按平台对账、按车场拆分。
 */
public enum PayRecordPlatform {
    WECHAT_PAY,
    ALIPAY_PAY,
    CASH;

    public static PayRecordPlatform from(PaymentMethod method) {
        if (method == PaymentMethod.ALIPAY_PAY) {
            return ALIPAY_PAY;
        }
        return WECHAT_PAY;
    }
}

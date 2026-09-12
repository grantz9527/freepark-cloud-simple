package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.PaymentOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * C 端在线缴款单视图。
 *
 * @param payNo         缴款单号（对外展示/对账用）
 * @param plateNumber   车牌号（快照，大写）
 * @param plateColor    车牌颜色枚举名（快照，null 表示不限颜色）
 * @param amountYuan     缴款总额（元）= 拆分明细金额之和
 * @param method        缴款渠道枚举名
 * @param status        状态：PENDING 待缴 / PAID 已缴 / CLOSED 已关闭
 * @param mock          是否本地联调模式（true 时由用户端调用确认接口完成缴款）
 * @param transactionId 渠道流水号（联调模式写入本地流水号；未缴款为 null）
 * @param payTime       缴款成功时间（未缴款为 null）
 * @param returnUrl     用户端缴款结果页（用户端基础地址 + /pay/{payNo}，供渠道同步跳回）
 * @param items         按流水拆分的明细
 * @param wxPay         微信 JSAPI 调起参数（仅真实微信下单返回；查询接口为 null）
 */
public record PaymentOrderView(
        String payNo,
        String plateNumber,
        String plateColor,
        BigDecimal amountYuan,
        String method,
        String status,
        boolean mock,
        String transactionId,
        LocalDateTime payTime,
        String returnUrl,
        List<PaymentItemView> items,
        WeChatJsapiPayView wxPay) {

    public static PaymentOrderView from(PaymentOrder payment, List<PaymentItemView> items) {
        return from(payment, items, null, null);
    }

    public static PaymentOrderView from(PaymentOrder payment, List<PaymentItemView> items, String returnUrl) {
        return from(payment, items, returnUrl, null);
    }

    public static PaymentOrderView from(PaymentOrder payment, List<PaymentItemView> items,
                                        String returnUrl, WeChatJsapiPayView wxPay) {
        return new PaymentOrderView(
                payment.getPayNo(),
                payment.getPlateNumber(),
                payment.getPlateColor() == null ? null : payment.getPlateColor().name(),
                payment.getAmountYuan(),
                payment.getMethod() == null ? null : payment.getMethod().name(),
                payment.getStatus() == null ? null : payment.getStatus().name(),
                payment.isMock(),
                payment.getTransactionId(),
                payment.getPayTime(),
                returnUrl,
                items,
                wxPay);
    }
}

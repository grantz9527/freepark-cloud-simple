package com.freepark.cloud.simple.parking.dto;

import java.util.List;

/**
 * C 端在线缴款下单请求。
 *
 * @param plateNumber 车牌号
 * @param plateColor  可选车牌颜色枚举名（BLUE/GREEN/…）：同一车牌存在多色记录时只缴该颜色，缺省不限颜色
 * @param method      缴款渠道枚举名（与站点「收费方式」配置一致，如 WECHAT_PAY/ALIPAY_PAY）
 * @param wxCode      微信网页授权 code（仅 WECHAT_PAY 真实下单需要；模拟支付可空）
 * @param sessionIds  要缴纳的停车流水 ID；系统配置为强制全部支付时忽略，允许勾选时必填
 */
public record CreatePaymentRequest(
        String plateNumber,
        String plateColor,
        String method,
        String wxCode,
        List<Long> sessionIds) {
}

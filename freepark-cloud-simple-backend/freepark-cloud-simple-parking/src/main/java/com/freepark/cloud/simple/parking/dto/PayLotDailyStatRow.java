package com.freepark.cloud.simple.parking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 车场按日收费统计一行：站点时区下的自然日 × 车场。
 *
 * @param payYuan    当日该场成功收款合计
 * @param wechatYuan 其中微信支付
 * @param alipayYuan 其中支付宝
 * @param cashYuan   其中现金收款
 * @param refundYuan 当日该场成功退款合计
 * @param netYuan    实收 = 收款 − 退款
 */
public record PayLotDailyStatRow(
        LocalDate statDate,
        Long lotId,
        String lotName,
        BigDecimal payYuan,
        BigDecimal wechatYuan,
        BigDecimal alipayYuan,
        BigDecimal cashYuan,
        BigDecimal refundYuan,
        BigDecimal netYuan) {
}

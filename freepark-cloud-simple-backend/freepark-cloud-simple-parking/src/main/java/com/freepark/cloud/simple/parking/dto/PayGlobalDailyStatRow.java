package com.freepark.cloud.simple.parking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 全局按日收费统计一行：站点时区下的自然日，全车场合计。
 */
public record PayGlobalDailyStatRow(
        LocalDate statDate,
        BigDecimal payYuan,
        BigDecimal wechatYuan,
        BigDecimal alipayYuan,
        BigDecimal cashYuan,
        BigDecimal refundYuan,
        BigDecimal netYuan) {
}

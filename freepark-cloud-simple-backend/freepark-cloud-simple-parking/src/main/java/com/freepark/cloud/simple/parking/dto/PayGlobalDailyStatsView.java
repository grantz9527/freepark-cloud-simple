package com.freepark.cloud.simple.parking.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 全局按日收费统计分页结果；合计为当前筛选条件下全部行的汇总（不随分页变化）。
 */
public record PayGlobalDailyStatsView(
        List<PayGlobalDailyStatRow> list,
        long total,
        int page,
        int size,
        BigDecimal payYuan,
        BigDecimal refundYuan,
        BigDecimal netYuan) {
}

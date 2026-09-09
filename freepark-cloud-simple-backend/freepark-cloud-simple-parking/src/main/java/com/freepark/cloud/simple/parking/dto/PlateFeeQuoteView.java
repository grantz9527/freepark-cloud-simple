package com.freepark.cloud.simple.parking.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * C 端查费结果：指定车牌当前的费用信息（全部车场口径）。
 *
 * @param plateNumber 查询车牌（大写）
 * @param items       费用明细：在场（含免费停放中）与历史未结记录，按入场时间倒序
 * @param totalAmount 合计应收（元）：在场估算 &gt; 0 + 历史未付清应收快照，无待缴为 0
 * @param colors      该车牌下存在记录的车牌颜色（枚举名，按 BLUE/YELLOW/GREEN/… 序）：
 *                    未传 plateColor 时可用它提示“同一车牌存在多个颜色”，
 *                    传了 plateColor 且无结果时用它提示可切换的颜色
 */
public record PlateFeeQuoteView(
        String plateNumber,
        List<PlateFeeItemView> items,
        BigDecimal totalAmount,
        List<String> colors) {
}

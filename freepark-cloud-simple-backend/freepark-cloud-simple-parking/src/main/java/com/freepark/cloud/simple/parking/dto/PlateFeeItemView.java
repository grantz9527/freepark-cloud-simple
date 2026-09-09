package com.freepark.cloud.simple.parking.dto;

import java.math.BigDecimal;

/**
 * C 端查费明细项：一条在停（ONGOING）或历史未结（SETTLED）的停车记录。
 *
 * @param type         ONGOING 在场 / SETTLED 已出场未结
 * @param lotName      车场名称（快照，可能为空）
 * @param plateColor   车牌颜色枚举名（BLUE/GREEN/…）；记录缺失为 null
 * @param entryText    入场时间（yyyy-MM-dd HH:mm，站点时区）
 * @param exitText     出场时间（yyyy-MM-dd HH:mm），ONGOING 为 null
 * @param durationText 已停/停车时长中文文本（如「2小时15分」）
 * @param amount       金额（元）：ONGOING 为按入场至今的估算应收（免费停放中为 0）；
 *                     SETTLED 为出场结算的应收快照（仅未付清且大于 0 的记录）
 */
public record PlateFeeItemView(
        String type,
        String lotName,
        String plateColor,
        String entryText,
        String exitText,
        String durationText,
        BigDecimal amount) {
}

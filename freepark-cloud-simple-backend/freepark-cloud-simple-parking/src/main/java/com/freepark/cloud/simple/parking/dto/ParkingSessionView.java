package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import com.freepark.cloud.simple.parking.entity.ParkingPayStatus;
import com.freepark.cloud.simple.parking.entity.ParkingSession;
import com.freepark.cloud.simple.parking.entity.ParkingSessionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 停车流水视图。
 *
 * @param feeYuan      应收金额快照（元），列表查询不重算；未计费为 null
 * @param paidAmountYuan 累计已支付金额（元）：通过停车订单收款入账后累加；无收款为 0
 * @param pendingYuan  该流水「未支付/待支付」订单金额（元）合计：下单未收款会占用可收口径，防止重复下单
 * @param payableYuan  剩余应付（元）= 应收快照 − 累计已支付 − 待付订单；无应收快照为 null（在场未快照不可结算）
 * @param parkedMinutes 停车时长（分钟）：已出场 = 出场−入场；在场 = 当前时刻−入场（随刷新更新）；
 *                     已作废 = null
 */
public record ParkingSessionView(
        Long id,
        Long lotId,
        String lotName,
        String plateNumber,
        String plateColor,
        String status,
        LocalDateTime entryTime,
        Long entryLaneId,
        String entryLaneName,
        Long entryRecognitionId,
        String entryImage,
        LocalDateTime exitTime,
        Long exitLaneId,
        String exitLaneName,
        Long exitRecognitionId,
        String exitImage,
        BigDecimal feeYuan,
        String payStatus,
        LocalDateTime payTime,
        Long parkedMinutes,
        BigDecimal paidAmountYuan,
        BigDecimal pendingYuan,
        BigDecimal payableYuan,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ParkingSessionView from(ParkingSession session) {
        return from(session, BigDecimal.ZERO);
    }

    /**
     * 组装视图：{@code pendingYuan} 为调用方查询到的该流水待付订单合计
     * （列表批量查询时为准确值；单条快捷场景传 0 不影响应收/已收字段）。
     */
    public static ParkingSessionView from(ParkingSession session, BigDecimal pendingYuan) {
        BigDecimal paid = session.paidAmountOrZero();
        BigDecimal pending = pendingYuan == null ? BigDecimal.ZERO : pendingYuan;
        return new ParkingSessionView(
                session.getId(),
                session.getLotId(),
                session.getLotName(),
                session.getPlateNumber(),
                session.getPlateColor() == null ? null : session.getPlateColor().name(),
                session.getStatus() == null ? null : session.getStatus().name(),
                session.getEntryTime(),
                session.getEntryLaneId(),
                session.getEntryLaneName(),
                session.getEntryRecognitionId(),
                session.getEntryImage(),
                session.getExitTime(),
                session.getExitLaneId(),
                session.getExitLaneName(),
                session.getExitRecognitionId(),
                session.getExitImage(),
                session.getFeeYuan(),
                payStatus(session, paid),
                session.getPayTime(),
                parkedMinutes(session),
                paid,
                pending,
                payableAmount(session, pending),
                session.getCreatedAt(),
                session.getUpdatedAt());
    }

    /**
     * 支付状态（展示口径）：
     * <ul>
     *   <li>已作废 → null；</li>
     *   <li>在场（未结算离场）→ 已收款视为「部分支付」，否则「未支付」；</li>
     *   <li>已出场且应收为 0 元 → 免缴费（FREE，仅展示派生，不落库）；</li>
     *   <li>已出场且有真实收款（累计已支付 &gt; 0）：累计已支付 ≥ 应收 → 已支付，否则 → 部分支付；</li>
     *   <li>已出场无金额记录 → 兼容历史登记状态，未登记视为未支付。</li>
     * </ul>
     */
    private static String payStatus(ParkingSession session, BigDecimal paid) {
        if (session.getStatus() == ParkingSessionStatus.VOIDED) {
            return null;
        }
        if (session.getStatus() != ParkingSessionStatus.CLOSED) {
            return paid.signum() > 0
                    ? ParkingPayStatus.PARTIAL.name()
                    : ParkingPayStatus.UNPAID.name();
        }
        BigDecimal fee = session.getFeeYuan();
        if (fee != null && fee.signum() == 0) {
            return "FREE";
        }
        if (paid.signum() > 0) {
            return fee != null && paid.compareTo(fee) >= 0
                    ? ParkingPayStatus.PAID.name()
                    : ParkingPayStatus.PARTIAL.name();
        }
        return session.getPayStatus() == null
                ? ParkingPayStatus.UNPAID.name()
                : session.getPayStatus().name();
    }

    /** 剩余应付（基于应收快照的口径）：应收快照为空返回 null；否则为 max(0, 应收 − 已付 − 待付订单)。 */
    private static BigDecimal payableAmount(ParkingSession session, BigDecimal pending) {
        BigDecimal fee = session.getFeeYuan();
        if (fee == null) {
            return null;
        }
        BigDecimal payable = fee.subtract(session.paidAmountOrZero()).subtract(pending);
        return payable.signum() > 0 ? payable : BigDecimal.ZERO;
    }

    /** 停车时长（分钟）：VOIDED 无意义返回 null；否则取「出场（或当前）−入场」，向下不取负。 */
    private static Long parkedMinutes(ParkingSession session) {
        if (session.getStatus() == ParkingSessionStatus.VOIDED || session.getEntryTime() == null) {
            return null;
        }
        LocalDateTime end = session.getExitTime() != null
                ? session.getExitTime()
                : SiteZoneTimes.nowUtc();
        long minutes = ChronoUnit.MINUTES.between(session.getEntryTime(), end);
        return Math.max(0L, minutes);
    }
}

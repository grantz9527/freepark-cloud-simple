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
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ParkingSessionView from(ParkingSession session) {
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
                payStatus(session),
                session.getPayTime(),
                parkedMinutes(session),
                session.getCreatedAt(),
                session.getUpdatedAt());
    }

    /**
     * 支付状态（展示口径）：
     * 已作废 → null；在场（未结算离场）→ 未支付；已出场且应收为 0 元 → 免缴费（FREE，仅展示派生，不落库）；
     * 已出场其余情况取登记状态，历史未登记视为未支付。
     */
    private static String payStatus(ParkingSession session) {
        if (session.getStatus() == ParkingSessionStatus.VOIDED) {
            return null;
        }
        if (session.getStatus() != ParkingSessionStatus.CLOSED) {
            return ParkingPayStatus.UNPAID.name();
        }
        BigDecimal fee = session.getFeeYuan();
        if (fee != null && fee.signum() == 0) {
            return "FREE";
        }
        return session.getPayStatus() == null
                ? ParkingPayStatus.UNPAID.name()
                : session.getPayStatus().name();
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

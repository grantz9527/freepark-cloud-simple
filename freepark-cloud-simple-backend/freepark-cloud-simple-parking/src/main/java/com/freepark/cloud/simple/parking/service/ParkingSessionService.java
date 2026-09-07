package com.freepark.cloud.simple.parking.service;

import com.freepark.cloud.simple.billing.service.BillingSessionChargeService;
import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.time.SiteZoneProvider;
import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.parking.dto.CreateParkingSessionRequest;
import com.freepark.cloud.simple.parking.dto.ParkingSessionView;
import com.freepark.cloud.simple.parking.dto.UpdateParkingSessionRequest;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.entity.ParkingPayStatus;
import com.freepark.cloud.simple.parking.entity.ParkingSession;
import com.freepark.cloud.simple.parking.entity.ParkingSessionStatus;
import com.freepark.cloud.simple.parking.entity.PlateColor;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.parking.repository.ParkingSessionRepository;
import com.freepark.cloud.simple.user.service.AdminGuard;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 停车流水服务：列表筛选、has-open 查询、手动新增（入场）、编辑（可修正入场/自动关场）、作废。
 * 同一车场同一车牌仅保留一条在场流水，重复入场时旧在场流水自动作废。
 */
@Service
public class ParkingSessionService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_PLATE_LENGTH = 20;
    private static final int MAX_TEXT_LENGTH = 120;

    private final ParkingSessionRepository sessions;
    private final ParkingLotRepository lots;
    private final AdminGuard adminGuard;
    private final BillingSessionChargeService chargeService;
    private final SiteZoneProvider siteZoneProvider;

    public ParkingSessionService(ParkingSessionRepository sessions,
                                 ParkingLotRepository lots,
                                 AdminGuard adminGuard,
                                 BillingSessionChargeService chargeService,
                                 SiteZoneProvider siteZoneProvider) {
        this.sessions = sessions;
        this.lots = lots;
        this.adminGuard = adminGuard;
        this.chargeService = chargeService;
        this.siteZoneProvider = siteZoneProvider;
    }

    @Transactional(readOnly = true)
    public PageResult<ParkingSessionView> listSessions(Long lotId, String keyword,
                                                       ParkingSessionStatus status,
                                                       LocalDate startDate, LocalDate endDate,
                                                       int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Specification<ParkingSession> spec = buildSpec(lotId, keyword, status, startDate, endDate);
        Page<ParkingSession> result = sessions.findAll(spec,
                PageRequest.of(safePage - 1, safeSize,
                        Sort.by(Sort.Direction.DESC, "entryTime")));
        List<ParkingSessionView> items = result.getContent().stream()
                .map(ParkingSessionView::from).toList();
        return PageResult.of(items, result.getTotalElements(), safePage, safeSize);
    }

    @Transactional(readOnly = true)
    public boolean hasOpenSession(Long lotId, String plateNumber) {
        if (lotId == null || !StringUtils.hasText(plateNumber)) {
            return false;
        }
        return sessions.existsByLotIdAndPlateNumberIgnoreCaseAndStatus(
                lotId, plateNumber.trim(), ParkingSessionStatus.OPEN);
    }

    /** 手动新增在场流水（入场）。重复在场流水自动作废，保证一辆车仅一条在场流水。 */
    @Transactional
    public ParkingSessionView createSession(CreateParkingSessionRequest request) {
        adminGuard.requireEnabledAdmin();
        if (request == null || request.lotId() == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        ParkingLot lot = requireLot(request.lotId());
        String plateNumber = requirePlate(request.plateNumber());
        voidOpenSessions(lot.getId(), plateNumber);

        ParkingSession session = new ParkingSession();
        session.setLotId(lot.getId());
        session.setLotName(lot.getName());
        session.setPlateNumber(plateNumber);
        session.setPlateColor(request.plateColor() == null ? PlateColor.BLUE : request.plateColor());
        session.setStatus(ParkingSessionStatus.OPEN);
        session.setEntryTime(request.entryTime() == null ? SiteZoneTimes.nowUtc() : request.entryTime());
        session.setEntryLaneId(request.entryLaneId());
        session.setEntryLaneName(normalizeOptional(request.entryLaneName()));
        session.setEntryImage(normalizeOptional(request.entryImage()));
        return ParkingSessionView.from(sessions.save(session));
    }

    /**
     * 编辑流水：修正入场信息；在场流水传入出场信息则自动关场；
     * 已出场流水可修正出场信息。已作废流水不可编辑。
     */
    @Transactional
    public ParkingSessionView updateSession(Long sessionId, UpdateParkingSessionRequest request) {
        adminGuard.requireEnabledAdmin();
        ParkingSession session = requireSession(sessionId);
        if (session.getStatus() == ParkingSessionStatus.VOIDED) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        if (request != null) {
            if (StringUtils.hasText(request.plateNumber())) {
                session.setPlateNumber(requirePlate(request.plateNumber()));
            }
            if (request.plateColor() != null) {
                session.setPlateColor(request.plateColor());
            }
            if (request.entryTime() != null) {
                session.setEntryTime(request.entryTime());
            }
            if (request.entryLaneId() != null) {
                session.setEntryLaneId(request.entryLaneId());
            }
            if (request.entryLaneName() != null) {
                session.setEntryLaneName(normalizeOptional(request.entryLaneName()));
            }
            if (request.entryImage() != null) {
                session.setEntryImage(normalizeOptional(request.entryImage()));
            }
            if (request.exitTime() != null) {
                if (!request.exitTime().isAfter(session.getEntryTime())) {
                    throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
                }
                if (session.getStatus() == ParkingSessionStatus.OPEN) {
                    session.closeWithExit(request.exitTime(),
                            request.exitLaneId(), normalizeOptional(request.exitLaneName()),
                            null, normalizeOptional(request.exitImage()));
                } else {
                    session.setExitTime(request.exitTime());
                    if (request.exitLaneId() != null) {
                        session.setExitLaneId(request.exitLaneId());
                    }
                    if (request.exitLaneName() != null) {
                        session.setExitLaneName(normalizeOptional(request.exitLaneName()));
                    }
                    if (request.exitImage() != null) {
                        session.setExitImage(normalizeOptional(request.exitImage()));
                    }
                }
            }
        }
        refreshFee(session);
        return ParkingSessionView.from(sessions.save(session));
    }

    /**
     * 作废流水（在场或已出场均可，重复作废幂等）。作废后应收金额一并清空。
     */
    @Transactional
    public ParkingSessionView voidSession(Long sessionId) {
        adminGuard.requireEnabledAdmin();
        ParkingSession session = requireSession(sessionId);
        if (session.getStatus() != ParkingSessionStatus.VOIDED) {
            session.markVoided();
            session.setFeeYuan(null);
            sessions.save(session);
        }
        return ParkingSessionView.from(session);
    }

    /**
     * 预览「重新算费」：只读计算应收金额但不落库，供前端弹窗确认。
     * 已出场流水按真实出场时间结算；在场流水按「入场 ~ 当前时刻」估算（截止当前）。
     * 前端弹窗确认后调用 {@link #recalculateSession} 才真正快照生效。
     */
    @Transactional(readOnly = true)
    public BigDecimal previewRecalculate(Long sessionId) {
        ParkingSession session = requireSession(sessionId);
        requireRecalculable(session);
        return computeFee(session);
    }

    /**
     * 手动「重新算费」：已出场流水按出场时间、在场流水按「入场 ~ 当前时刻」估算，快照应收金额。
     * 已作废流水不可重算。在场流水后续出场时，会按真实出场时间重算覆盖该估算值。
     */
    @Transactional
    public ParkingSessionView recalculateSession(Long sessionId) {
        adminGuard.requireEnabledAdmin();
        ParkingSession session = requireSession(sessionId);
        requireRecalculable(session);
        session.setFeeYuan(computeFee(session));
        return ParkingSessionView.from(sessions.save(session));
    }

    /**
     * 人工登记支付状态：仅已出场流水可登记。支付状态为独立快照，费用重算/编辑不自动改变它。
     * 登记为 PAID（已支付）时记录支付时间（重复登记已支付不覆盖原时间）。
     */
    @Transactional
    public ParkingSessionView markPayStatus(Long sessionId, ParkingPayStatus status) {
        adminGuard.requireEnabledAdmin();
        if (status == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        ParkingSession session = requireSession(sessionId);
        if (session.getStatus() != ParkingSessionStatus.CLOSED) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        session.setPayStatus(status);
        if (status == ParkingPayStatus.PAID) {
            if (session.getPayTime() == null) {
                session.setPayTime(SiteZoneTimes.nowUtc());
            }
        } else {
            session.setPayTime(null);
        }
        return ParkingSessionView.from(sessions.save(session));
    }

    /** 校验该流水可作为「重新算费」输入：已出场须有完整出入场区间，在场须有入场时间。 */
    private void requireRecalculable(ParkingSession session) {
        if (session.getStatus() == ParkingSessionStatus.VOIDED
                || session.getEntryTime() == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        if (session.getStatus() == ParkingSessionStatus.CLOSED
                && (session.getExitTime() == null
                || !session.getExitTime().isAfter(session.getEntryTime()))) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
    }

    /**
     * 结算应收金额：已出场用真实出场时间；在场用当前时刻作为临时终点（估算）。
     * 未命中绑定/区间不可结算/入场晚于终点时返回 null（未计费）。
     */
    private BigDecimal computeFee(ParkingSession session) {
        LocalDateTime exit = session.getStatus() == ParkingSessionStatus.CLOSED
                ? session.getExitTime()
                : SiteZoneTimes.nowUtc();
        if (session.getEntryTime() == null || exit == null
                || !exit.isAfter(session.getEntryTime())) {
            return null;
        }
        String color = session.getPlateColor() == null
                ? null
                : session.getPlateColor().name();
        return chargeService.chargeFor(session.getLotId(), color,
                session.getEntryTime(), exit);
    }

    /**
     * 快照应收金额：已出场按真实出场时间；在场按「入场 ~ 当前时刻」刷新估算值（编辑流水后避免旧估算滞留）。
     * 不可结算/不可计费时置为 null（未计费）。
     */
    private void refreshFee(ParkingSession session) {
        if (session.getStatus() == ParkingSessionStatus.VOIDED) {
            session.setFeeYuan(null);
            return;
        }
        session.setFeeYuan(computeFee(session));
    }

    /** 入场前将同车场同车牌残留的在场流水作废，保证一辆车仅保留一条新在场流水。 */
    private void voidOpenSessions(Long lotId, String plateNumber) {
        List<ParkingSession> openSessions = sessions
                .findAllByLotIdAndPlateNumberIgnoreCaseAndStatus(
                        lotId, plateNumber, ParkingSessionStatus.OPEN);
        for (ParkingSession open : openSessions) {
            open.markVoided();
            sessions.save(open);
        }
    }

    private Specification<ParkingSession> buildSpec(Long lotId, String keyword,
                                                    ParkingSessionStatus status,
                                                    LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (lotId != null) {
                predicates.add(cb.equal(root.get("lotId"), lotId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("plateNumber")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("lotName"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("entryLaneName"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("exitLaneName"), "")), like)));
            }
            if (startDate != null || endDate != null) {
                predicates.add(withinRange(root, cb, startDate, endDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 时间区间过滤：入场或出场任一落在所选站点墙钟日期内即命中；在场流水无出场时间，按入场时间判断。
     * 库内存储为 UTC 锚点，先将墙钟日期边界换算为锚点再比较（日期为空的一侧不限）。
     */
    private Predicate withinRange(Root<ParkingSession> root,
                                  CriteriaBuilder cb,
                                  LocalDate startDate, LocalDate endDate) {
        ZoneId zone = siteZoneProvider.currentZone();
        Path<LocalDateTime> entry = root.get("entryTime");
        Path<LocalDateTime> exit = root.get("exitTime");
        List<Predicate> entryIn = new ArrayList<>();
        List<Predicate> exitIn = new ArrayList<>();
        if (startDate != null) {
            LocalDateTime from = SiteZoneTimes.toUtcAnchor(LocalDateTime.of(startDate, LocalTime.MIN), zone);
            entryIn.add(cb.greaterThanOrEqualTo(entry, from));
            exitIn.add(cb.greaterThanOrEqualTo(exit, from));
        }
        if (endDate != null) {
            LocalDateTime to = SiteZoneTimes.toUtcAnchor(LocalDateTime.of(endDate.plusDays(1), LocalTime.MIN), zone);
            entryIn.add(cb.lessThan(entry, to));
            exitIn.add(cb.lessThan(exit, to));
        }
        return cb.or(
                cb.and(entryIn.toArray(new Predicate[0])),
                cb.and(exitIn.toArray(new Predicate[0])));
    }

    private ParkingLot requireLot(Long lotId) {
        return lots.findById(lotId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
    }

    private ParkingSession requireSession(Long sessionId) {
        return sessions.findById(sessionId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
    }

    private String requirePlate(String value) {
        String trimmed = value == null ? null : value.trim();
        if (!StringUtils.hasText(trimmed) || trimmed.length() > MAX_PLATE_LENGTH) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return trimmed;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return StringUtils.hasText(trimmed) && trimmed.length() <= MAX_TEXT_LENGTH ? trimmed : null;
    }
}

package com.freepark.cloud.simple.parking.service;

import com.freepark.cloud.simple.billing.service.BillingSessionChargeService;
import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.time.SiteZoneProvider;
import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.parking.dto.CreateParkingSessionRequest;
import com.freepark.cloud.simple.parking.dto.ParkingSessionView;
import com.freepark.cloud.simple.parking.dto.PayableQuoteView;
import com.freepark.cloud.simple.parking.dto.PlateFeeItemView;
import com.freepark.cloud.simple.parking.dto.PlateFeeQuoteView;
import com.freepark.cloud.simple.parking.dto.UpdateParkingSessionRequest;
import com.freepark.cloud.simple.parking.dto.VehicleArrearsResult;
import com.freepark.cloud.simple.parking.entity.DiscountVehicle;
import com.freepark.cloud.simple.parking.entity.LaneType;
import com.freepark.cloud.simple.parking.entity.LotArrearsScope;
import com.freepark.cloud.simple.parking.entity.ParkingLane;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.entity.ParkingOrder;
import com.freepark.cloud.simple.parking.entity.ParkingOrderStatus;
import com.freepark.cloud.simple.parking.entity.ParkingPayStatus;
import com.freepark.cloud.simple.parking.entity.ParkingSession;
import com.freepark.cloud.simple.parking.entity.ParkingSessionStatus;
import com.freepark.cloud.simple.parking.entity.PlateColor;
import com.freepark.cloud.simple.parking.event.ParkingSessionChangedEvent;
import com.freepark.cloud.simple.parking.event.PublicPlateFeeSnapshotEvent;
import com.freepark.cloud.simple.parking.repository.DiscountVehicleRepository;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.parking.repository.ParkingOrderRepository;
import com.freepark.cloud.simple.parking.repository.ParkingSessionRepository;
import com.freepark.cloud.simple.user.service.AdminGuard;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 停车流水服务：列表筛选、has-open 查询、手动新增（入场/整条补录）、编辑（可修正入场/自动关场）、作废。
 * 同一车场同一车牌仅保留一条在场流水；手动补录只写入本条记录，不自动作废其它流水。
 */
@Service
public class ParkingSessionService {

    private static final Logger log = LoggerFactory.getLogger(ParkingSessionService.class);

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_PLATE_LENGTH = 20;
    private static final int MAX_TEXT_LENGTH = 120;

    private final ParkingSessionRepository sessions;
    private final ParkingLotRepository lots;
    private final DiscountVehicleRepository discountVehicles;
    private final ParkingOrderRepository orders;
    private final AdminGuard adminGuard;
    private final BillingSessionChargeService chargeService;
    private final SiteZoneProvider siteZoneProvider;
    private final ApplicationEventPublisher events;

    public ParkingSessionService(ParkingSessionRepository sessions,
                                 ParkingLotRepository lots,
                                 DiscountVehicleRepository discountVehicles,
                                 ParkingOrderRepository orders,
                                 AdminGuard adminGuard,
                                 BillingSessionChargeService chargeService,
                                 SiteZoneProvider siteZoneProvider,
                                 ApplicationEventPublisher events) {
        this.sessions = sessions;
        this.lots = lots;
        this.discountVehicles = discountVehicles;
        this.orders = orders;
        this.adminGuard = adminGuard;
        this.chargeService = chargeService;
        this.siteZoneProvider = siteZoneProvider;
        this.events = events;
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
        List<ParkingSession> content = result.getContent();
        Map<Long, BigDecimal> pendingBySession = pendingOrdersAmounts(toSessionIds(content));
        List<ParkingSessionView> items = content.stream()
                .map(s -> ParkingSessionView.from(
                        s, pendingBySession.getOrDefault(s.getId(), BigDecimal.ZERO)))
                .toList();
        return PageResult.of(items, result.getTotalElements(), safePage, safeSize);
    }

    /**
     * 车费查询（单车欠费流水）：返回车牌（+可选车场）的全部「欠费」停车流水，
     * 含在场（OPEN，已产生估算费用）与已出场（CLOSED，已结算）的流水。
     * <p>欠费金额口径与订单体系一致：每条流水的欠费 = 应收 − 流水累计已支付 − 该流水待付订单合计，
     * 已付清或已由待付订单全覆盖的流水不计入欠费；免缴费（0 元）与历史已登记支付（无金额记录）不计入。
     * {@code totalAmount} 为该车全量欠费流水的欠费合计（不受分页影响），
     * 保证在场多次缴费「第二次仅需支付再次产生的金额」。
     */
    @Transactional(readOnly = true)
    public VehicleArrearsResult queryVehicleArrears(Long lotId, String plateNumber,
                                                    int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        if (!StringUtils.hasText(plateNumber)) {
            return new VehicleArrearsResult(List.of(), 0, safePage, safeSize, BigDecimal.ZERO);
        }
        String plate = plateNumber.trim().toUpperCase();
        List<ParkingSession> matched = sessions.findAll(buildVehicleArrearsSpec(lotId, plate));
        Map<Long, BigDecimal> pendingBySession = pendingOrdersAmounts(toSessionIds(matched));

        List<ParkingSessionView> owed = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ParkingSession session : matched) {
            BigDecimal fee = session.getFeeYuan();
            BigDecimal paid = session.paidAmountOrZero();
            BigDecimal pending = pendingBySession.getOrDefault(session.getId(), BigDecimal.ZERO);
            BigDecimal outstanding = fee == null ? BigDecimal.ZERO
                    : maxZero(fee.subtract(paid).subtract(pending));
            if (outstanding.signum() <= 0) {
                continue;
            }
            totalAmount = totalAmount.add(outstanding);
            owed.add(ParkingSessionView.from(session, pending));
        }
        int fromIndex = Math.min((safePage - 1) * safeSize, owed.size());
        int toIndex = Math.min(fromIndex + safeSize, owed.size());
        List<ParkingSessionView> items = new ArrayList<>(owed.subList(fromIndex, toIndex));
        return new VehicleArrearsResult(items, owed.size(), safePage, safeSize, totalAmount);
    }

    /**
     * 算费请求（供边缘节点调用）：返回指定车牌当前的欠费金额（元）。
     * <ul>
     *   <li>请求带 {@code lotCode} 时按该车场的「欠费统计范围」统计：
     *       范围 LOT 只统计本车场的欠费，范围 GLOBAL 则跨全部车场统计；车场不存在返回 0（不拦截）；</li>
     *   <li>未带 {@code lotCode} 时按全部车场统计（兼容直接测试/无法定位车场的调用）；</li>
     *   <li>欠费口径：每条流水的欠费 = 应收 − 流水累计已支付 − 该流水待付订单合计（未付清则计入）；
     *       已出场（CLOSED）应收快照扣减；在场（OPEN）按「入场 ~ 当前时刻」的估算应收扣减
     *       （命中优惠车辆时应用每次入场免费时长）；历史登记已支付（无金额记录）视为已结清。</li>
     * </ul>
     * 统计不修改任何快照，仅返回当前欠费合计。
     */
    @Transactional(readOnly = true)
    public BigDecimal quoteArrearsAmount(String lotCode, String plateNumber) {
        String plate = plateNumber == null ? null : plateNumber.trim().toUpperCase();
        if (!StringUtils.hasText(plate)) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        Long lotId = null;
        if (StringUtils.hasText(lotCode)) {
            ParkingLot lot = lots.findByCode(lotCode.trim()).orElse(null);
            if (lot == null) {
                return BigDecimal.ZERO;
            }
            lotId = lot.getArrearsScope() == LotArrearsScope.LOT ? lot.getId() : null;
        }
        List<ParkingSession> matched = sessions.findAll(buildVehicleLatestSpec(lotId, plate));
        Map<Long, BigDecimal> pendingBySession = pendingOrdersAmounts(toSessionIds(matched));
        BigDecimal total = BigDecimal.ZERO;
        for (ParkingSession session : matched) {
            BigDecimal paid = session.paidAmountOrZero();
            BigDecimal pending = pendingBySession.getOrDefault(session.getId(), BigDecimal.ZERO);
            if (session.getStatus() == ParkingSessionStatus.OPEN) {
                BigDecimal estimated = computeFee(session);
                if (estimated != null) {
                    total = total.add(maxZero(estimated.subtract(paid).subtract(pending)));
                }
            } else if (session.getStatus() == ParkingSessionStatus.CLOSED) {
                if (session.getPayStatus() == ParkingPayStatus.PAID && paid.signum() == 0) {
                    // 历史登记已支付（无金额记录）仍视为已结清
                    continue;
                }
                BigDecimal fee = session.getFeeYuan();
                if (fee != null) {
                    total = total.add(maxZero(fee.subtract(paid).subtract(pending)));
                }
            }
        }
        return total;
    }

    /**
     * C 端公开查费：返回指定车牌当前费用信息（全部车场口径），供用户端网页查询。
     * <ul>
     *   <li>在场（OPEN）流水全部返回：金额为按入场至今的估算应收（免费/未计费为 0，用于展示“免费停放中”）；</li>
     *   <li>已出场（CLOSED）仅返回未付清且应收快照 &gt; 0 的记录（历史欠费）；已支付与 0 元单不展示；</li>
     *   <li>可选 {@code plateColor}：同一车牌可能存在不同颜色的记录（识别误差/换车），
     *       传值后仅统计该颜色的流水（未记录颜色的历史流水一并忽略），避免金额混淆；</li>
     *   <li>返回 {@code colors}：该车牌实际存在的颜色清单，便于前端提示可切换的颜色；</li>
     *   <li>{@code totalAmount} 合计口径与 {@link #quoteArrearsAmount(String, String)} 一致。</li>
     * </ul>
     * 只读查询并立即返回；命中流水的应收快照在事务提交后异步回写，不拖慢响应。
     */
    @Transactional(readOnly = true)
    public PlateFeeQuoteView queryPublicPlateFee(String plateNumber, PlateColor plateColor) {
        String plate = normalizePublicPlate(plateNumber);
        PlateFeeSnapshot snapshot = collectPlateFee(plate, plateColor);
        LocalDateTime now = SiteZoneTimes.nowUtc();
        List<PlateFeeItemView> items = new ArrayList<>(snapshot.entries().size());
        BigDecimal total = BigDecimal.ZERO;
        List<Long> sessionIds = new ArrayList<>(snapshot.entries().size());
        for (PlateFeeEntry entry : snapshot.entries()) {
            items.add(toPlateFeeItem(entry.session(), entry.ongoing(), entry.amount(), now));
            total = total.add(entry.amount());
            if (entry.session().getId() != null) {
                sessionIds.add(entry.session().getId());
            }
        }
        if (!sessionIds.isEmpty()) {
            events.publishEvent(new PublicPlateFeeSnapshotEvent(List.copyOf(sessionIds)));
        }
        return new PlateFeeQuoteView(plate, items, total, snapshot.colors());
    }

    /**
     * C 端查费后异步调用：按当前计费规则重算并回写流水应收快照（金额未变则跳过）。
     * 供 {@link PublicPlateFeeSnapshotListener} 在查费事务提交后执行。
     */
    @Transactional
    public void refreshFeeSnapshotsAfterPublicQuery(Collection<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return;
        }
        // 去重，保持稳定顺序
        Set<Long> unique = new LinkedHashSet<>();
        for (Long id : sessionIds) {
            if (id != null) {
                unique.add(id);
            }
        }
        for (Long sessionId : unique) {
            ParkingSession session = sessions.findById(sessionId).orElse(null);
            if (session == null || session.getStatus() == ParkingSessionStatus.VOIDED) {
                continue;
            }
            if (session.getEntryTime() == null) {
                continue;
            }
            if (session.getStatus() == ParkingSessionStatus.CLOSED
                    && (session.getExitTime() == null
                    || !session.getExitTime().isAfter(session.getEntryTime()))) {
                continue;
            }
            BigDecimal next = computeFee(session);
            if (sameMoney(session.getFeeYuan(), next)) {
                continue;
            }
            session.setFeeYuan(next);
            saveAndPush(session);
        }
    }

    private static boolean sameMoney(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.compareTo(right) == 0;
    }

    /**
     * C 端在线缴费：列出指定车牌当前仍需缴纳的流水（在场估算 + 已出场未结），
     * 口径与 {@link #queryPublicPlateFee} 完全一致（只扣除管理端人工下单的待缴订单），
     * 返回按流水拆分的可缴金额快照，供缴款下单时逐笔生成订单。
     */
    @Transactional(readOnly = true)
    public List<PayableQuoteView> listPayableQuotes(String plateNumber, PlateColor plateColor) {
        String plate = normalizePublicPlate(plateNumber);
        PlateFeeSnapshot snapshot = collectPlateFee(plate, plateColor);
        List<PayableQuoteView> quotes = new ArrayList<>();
        for (PlateFeeEntry entry : snapshot.entries()) {
            if (entry.amount().signum() <= 0) {
                continue;
            }
            quotes.add(new PayableQuoteView(entry.session().getId(), entry.receivable(),
                    entry.paid(), entry.pending(), entry.amount()));
        }
        return quotes;
    }

    private static String normalizePublicPlate(String plateNumber) {
        String plate = plateNumber == null ? null : plateNumber.trim().toUpperCase();
        if (!StringUtils.hasText(plate)) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return plate;
    }

    /** 单条流水的 C 端费用口径：金额为正表示仍需缴纳，0 表示当前无需缴纳（在场免费/已缴清）。 */
    private record PlateFeeEntry(ParkingSession session, boolean ongoing,
                                 BigDecimal receivable, BigDecimal paid,
                                 BigDecimal pending, BigDecimal amount) {
    }

    /** 车牌费用快照：entries 按入场时间倒序（无入场时间的排最后），colors 为该车牌实际存在的颜色清单。 */
    private record PlateFeeSnapshot(List<PlateFeeEntry> entries, List<String> colors) {
    }

    /**
     * 按车牌聚合 C 端费用口径（可选颜色过滤）：
     * <ul>
     *   <li>在场（OPEN）全部保留：金额 = 估算应收 − 已缴 − 管理端人工待缴订单（可为 0，用于展示“免费停放中”）；</li>
     *   <li>已出场（CLOSED）仅保留应收快照 &gt; 0 且未缴清的记录；</li>
     *   <li>只扣除「管理端人工下单」的待缴订单：C 端在线缴费自身拆出的待缴订单不占用车主可见金额。</li>
     * </ul>
     */
    private PlateFeeSnapshot collectPlateFee(String plate, PlateColor plateColor) {
        List<ParkingSession> matched = sessions.findAll(buildVehicleLatestSpec(null, plate));
        // 该车牌存在的颜色集合（无论当前是否按颜色过滤，均用于前端提示可切换的颜色）
        Set<PlateColor> colorSet = EnumSet.noneOf(PlateColor.class);
        for (ParkingSession session : matched) {
            if (session.getPlateColor() != null) {
                colorSet.add(session.getPlateColor());
            }
        }
        Map<Long, BigDecimal> pendingBySession = manualPendingOrdersAmounts(toSessionIds(matched));
        List<PlateFeeEntry> entries = new ArrayList<>();
        for (ParkingSession session : matched) {
            if (plateColor != null && session.getPlateColor() != plateColor) {
                // 指定了颜色：只统计该颜色流水（历史未记颜色的流水无法确认，一并排除）
                continue;
            }
            BigDecimal paid = session.paidAmountOrZero();
            BigDecimal pending = pendingBySession.getOrDefault(session.getId(), BigDecimal.ZERO);
            if (session.getStatus() == ParkingSessionStatus.OPEN) {
                BigDecimal estimated = computeFee(session);
                BigDecimal receivable = estimated == null ? BigDecimal.ZERO : estimated;
                entries.add(new PlateFeeEntry(session, true, receivable, paid, pending,
                        maxZero(receivable.subtract(paid).subtract(pending))));
            } else if (session.getStatus() == ParkingSessionStatus.CLOSED) {
                if (isHistoricallySettled(session)) {
                    // 历史登记已缴（无金额记录）不再展示
                    continue;
                }
                BigDecimal fee = session.getFeeYuan();
                if (fee == null) {
                    continue;
                }
                BigDecimal amount = maxZero(fee.subtract(paid).subtract(pending));
                if (amount.signum() <= 0) {
                    continue;
                }
                entries.add(new PlateFeeEntry(session, false, fee, paid, pending, amount));
            }
        }
        entries.sort(Comparator.comparing(
                (PlateFeeEntry entry) -> entry.session().getEntryTime(),
                Comparator.nullsLast(Comparator.reverseOrder())));
        List<String> colorNames = new ArrayList<>();
        for (PlateColor value : PlateColor.values()) {
            if (colorSet.contains(value)) {
                colorNames.add(value.name());
            }
        }
        return new PlateFeeSnapshot(entries, colorNames);
    }

    private static final DateTimeFormatter FEE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static PlateFeeItemView toPlateFeeItem(ParkingSession session, boolean ongoing,
                                                   BigDecimal amount, LocalDateTime now) {
        LocalDateTime end = ongoing ? now : session.getExitTime();
        String durationText = "";
        if (session.getEntryTime() != null && end != null && end.isAfter(session.getEntryTime())) {
            durationText = formatDuration(Duration.between(session.getEntryTime(), end));
        }
        return new PlateFeeItemView(
                session.getId(),
                ongoing ? "ONGOING" : "SETTLED",
                session.getLotName(),
                session.getPlateColor() == null ? null : session.getPlateColor().name(),
                session.getEntryTime() == null ? "" : FEE_TIME_FORMATTER.format(session.getEntryTime()),
                ongoing || session.getExitTime() == null
                        ? null
                        : FEE_TIME_FORMATTER.format(session.getExitTime()),
                durationText,
                amount);
    }

    private static String formatDuration(Duration duration) {
        long minutes = Math.max(0, duration.toMinutes());
        long days = minutes / 1440;
        minutes %= 1440;
        long hours = minutes / 60;
        minutes %= 60;
        if (days > 0) {
            return days + "天" + hours + "小时";
        }
        if (hours > 0 && minutes > 0) {
            return hours + "小时" + minutes + "分";
        }
        if (hours > 0) {
            return hours + "小时";
        }
        if (minutes > 0) {
            return minutes + "分钟";
        }
        return "不足1分钟";
    }

    /**
     * 车费查询辅助：刷新车牌（+可选车场）最近一笔停车流水的费用。
     * 在场（OPEN）按「入场 ~ 当前时刻」估算；已出场（CLOSED）按真实出场时间重算；
     * 均快照应收金额，保证查询时展示的是按当前计费配置（含优惠车辆免费时长）的最新结果。
     * 无可用流水时返回 {@code null}。
     */
    @Transactional
    public ParkingSessionView recalcLatestSession(Long lotId, String plateNumber) {
        adminGuard.requireEnabledAdmin();
        if (!StringUtils.hasText(plateNumber)) {
            return null;
        }
        String plate = plateNumber.trim().toUpperCase();
        List<ParkingSession> first = sessions.findAll(
                buildVehicleLatestSpec(lotId, plate),
                PageRequest.of(0, 1,
                        Sort.by(Sort.Direction.DESC, "entryTime"))).getContent();
        if (first.isEmpty()) {
            return null;
        }
        ParkingSession session = first.get(0);
        if (session.getEntryTime() == null
                || (session.getStatus() == ParkingSessionStatus.CLOSED
                && (session.getExitTime() == null
                || !session.getExitTime().isAfter(session.getEntryTime())))) {
            return null;
        }
        session.setFeeYuan(computeFee(session));
        return ParkingSessionView.from(saveAndPush(session));
    }

    /** 车费查询条件：可选车场 + 车牌精确匹配（忽略大小写）+ 在场或已出场。 */
    private Specification<ParkingSession> buildVehicleLatestSpec(Long lotId, String plate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (lotId != null) {
                predicates.add(cb.equal(root.get("lotId"), lotId));
            }
            predicates.add(cb.equal(cb.lower(root.get("plateNumber")), plate.toLowerCase()));
            predicates.add(root.get("status").in(List.of(
                    ParkingSessionStatus.OPEN, ParkingSessionStatus.CLOSED)));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** 车费查询条件：可选车场 + 车牌精确匹配（忽略大小写）+ 在场或已出场 + 应收 &gt; 0 且未付清。 */
    private Specification<ParkingSession> buildVehicleArrearsSpec(Long lotId, String plate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (lotId != null) {
                predicates.add(cb.equal(root.get("lotId"), lotId));
            }
            predicates.add(cb.equal(cb.lower(root.get("plateNumber")), plate.toLowerCase()));
            predicates.add(root.get("status").in(List.of(
                    ParkingSessionStatus.OPEN, ParkingSessionStatus.CLOSED)));
            Path<BigDecimal> fee = root.get("feeYuan");
            predicates.add(cb.isNotNull(fee));
            predicates.add(cb.greaterThan(fee, BigDecimal.ZERO));
            Path<ParkingPayStatus> pay = root.get("payStatus");
            // 未登记支付（null）展示为未支付；在场无支付状态同样视为欠费
            predicates.add(cb.or(cb.isNull(pay), cb.notEqual(pay, ParkingPayStatus.PAID)));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Transactional(readOnly = true)
    public boolean hasOpenSession(Long lotId, String plateNumber) {
        if (lotId == null || !StringUtils.hasText(plateNumber)) {
            return false;
        }
        return sessions.existsByLotIdAndPlateNumberIgnoreCaseAndStatus(
                lotId, plateNumber.trim(), ParkingSessionStatus.OPEN);
    }

    /**
     * 手动新增流水：不填出场信息时新增一条在场（OPEN）流水（入场）；
     * 填了出场信息则直接生成一条已出场（CLOSED）的完整流水，并按真实出入场结算应收。
     * 手动补录只写入本条记录：不自动作废、也不因同车场同车牌的在场流水而拦截整条补录；
     * 仅当要再补一条「入场」而该车已有在场流水时提示（避免同车场同车牌出现两条在场）。
     */
    @Transactional
    public ParkingSessionView createSession(CreateParkingSessionRequest request) {
        adminGuard.requireEnabledAdmin();
        if (request == null || request.lotId() == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        ParkingLot lot = requireLot(request.lotId());
        String plateNumber = requirePlate(request.plateNumber());
        LocalDateTime entryTime = request.entryTime() == null
                ? SiteZoneTimes.nowUtc()
                : request.entryTime();
        LocalDateTime exitTime = request.exitTime();
        if (exitTime != null && !exitTime.isAfter(entryTime)) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        if (exitTime == null
                && sessions.existsByLotIdAndPlateNumberIgnoreCaseAndStatus(
                lot.getId(), plateNumber, ParkingSessionStatus.OPEN)) {
            throw new BizException(400, MessageKeys.PARKING_SESSION_OPEN_ALREADY_EXISTS);
        }

        ParkingSession session = new ParkingSession();
        session.setLotId(lot.getId());
        session.setLotName(lot.getName());
        session.setPlateNumber(plateNumber);
        session.setPlateColor(request.plateColor() == null ? PlateColor.BLUE : request.plateColor());
        session.setStatus(ParkingSessionStatus.OPEN);
        session.setEntryTime(entryTime);
        session.setEntryLaneId(request.entryLaneId());
        session.setEntryLaneName(normalizeOptional(request.entryLaneName()));
        session.setEntryImage(normalizeOptional(request.entryImage()));
        if (exitTime != null) {
            session.closeWithExit(exitTime,
                    request.exitLaneId(), normalizeOptional(request.exitLaneName()),
                    null, normalizeOptional(request.exitImage()));
            refreshFee(session);
        }
        bindEdgeNode(session, lot);
        return ParkingSessionView.from(saveAndPush(session));
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
        return ParkingSessionView.from(saveAndPush(session));
    }

    /**
     * 作废流水（在场或已出场均可，重复作废幂等）。作废后应收金额一并清空。
     * 该流水已产生收款记录或存在待付/已付订单时禁止作废，避免抹掉真实收款流水。
     */
    @Transactional
    public ParkingSessionView voidSession(Long sessionId) {
        adminGuard.requireEnabledAdmin();
        ParkingSession session = requireSession(sessionId);
        if (session.getStatus() != ParkingSessionStatus.VOIDED) {
            if (session.paidAmountOrZero().signum() > 0
                    || !orders.findBySessionIdAndStatus(sessionId, ParkingOrderStatus.PENDING).isEmpty()
                    || !orders.findBySessionIdAndStatus(sessionId, ParkingOrderStatus.PAID).isEmpty()
                    || !orders.findBySessionIdAndStatus(sessionId, ParkingOrderStatus.PARTIAL_REFUND).isEmpty()) {
                throw new BizException(400, MessageKeys.PARKING_SESSION_HAS_PAYMENTS);
            }
            session.markVoided();
            session.setFeeYuan(null);
            saveAndPush(session);
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
        return ParkingSessionView.from(saveAndPush(session));
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
     * 命中「优惠车辆」时每次入场免费 freeMinutes 分钟（自入场时刻顺延起算），其余照常计费。
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
        Integer freeMinutes = null;
        if (session.getLotId() != null && StringUtils.hasText(session.getPlateNumber())) {
            DiscountVehicle discount = discountVehicles
                    .findByLotIdAndPlateNumberIgnoreCaseAndEnabledTrue(
                            session.getLotId(), session.getPlateNumber())
                    .orElse(null);
            if (discount != null) {
                freeMinutes = discount.getFreeMinutes();
            }
        }
        String color = session.getPlateColor() == null
                ? null
                : session.getPlateColor().name();
        return chargeService.chargeFor(session.getLotId(), color,
                session.getEntryTime(), exit, freeMinutes);
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

    /** 当前应收口径：已出场取应收快照（真实出场已结算）；在场按「入场 ~ 当前时刻」实时估算（不落库）。 */
    public BigDecimal currentReceivable(ParkingSession session) {
        return session.getStatus() == ParkingSessionStatus.CLOSED
                ? session.getFeeYuan()
                : computeFee(session);
    }

    /**
     * 订单收款入账：把一笔「已支付」停车订单的金额累加到流水累计已支付，
     * 并按金额自动推导支付状态（付清 → 已支付、未满 → 部分支付；在场流水保持在场口径，
     * 待出场结算后按真实应收再推导）。调用方须在同一事务内先完成订单状态变更。
     */
    @Transactional
    public ParkingSession applyOrderReceivable(Long sessionId, BigDecimal paidAmount) {
        ParkingSession session = requireSession(sessionId);
        if (session.getStatus() == ParkingSessionStatus.VOIDED) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        session.setPaidAmountYuan(session.paidAmountOrZero().add(paidAmount));
        session.syncPayStatusFromMoney();
        return saveAndPush(session);
    }

    /**
     * 缴费离场：欠费拦截后开闸放行，把该出口通道所属车场的在场流水补成已出场。
     * 拦截当时不上报离场；入口欠费放行不关场。
     */
    @Transactional
    public void closeOpenSessionsAfterPaidExit(String plateNumber, PlateColor plateColor,
                                               List<ParkingLane> waitingLanes) {
        if (!StringUtils.hasText(plateNumber) || waitingLanes == null || waitingLanes.isEmpty()) {
            return;
        }
        String plate = plateNumber.trim().toUpperCase();
        LocalDateTime exitTime = SiteZoneTimes.nowUtc();
        Set<Long> closedLots = new LinkedHashSet<>();
        for (ParkingLane lane : waitingLanes) {
            if (lane == null || lane.getLaneType() == LaneType.ENTRANCE || lane.getLot() == null) {
                continue;
            }
            Long lotId = lane.getLot().getId();
            if (lotId == null || !closedLots.add(lotId)) {
                continue;
            }
            List<ParkingSession> opens = sessions.findAllByLotIdAndPlateNumberIgnoreCaseAndStatus(
                    lotId, plate, ParkingSessionStatus.OPEN);
            for (ParkingSession session : opens) {
                if (plateColor != null
                        && session.getPlateColor() != null
                        && plateColor != session.getPlateColor()) {
                    continue;
                }
                LocalDateTime at = exitTime;
                if (session.getEntryTime() != null && !at.isAfter(session.getEntryTime())) {
                    at = session.getEntryTime().plusMinutes(1);
                }
                session.closeWithExit(at, lane.getId(), lane.getName(), null, null);
                refreshFee(session);
                session.syncPayStatusFromMoney();
                saveAndPush(session);
                log.info("缴费离场已补出场 sessionId={} plate={} lot={} lane={}",
                        session.getId(), plate, session.getLotId(), lane.getCode());
            }
        }
    }

    /**
     * 订单退款回冲：从流水累计已支付中扣减退款金额（不低于 0），
     * 已出场流水按剩余已付重新推导支付状态。
     */
    @Transactional
    public ParkingSession applyOrderRefund(Long sessionId, BigDecimal refundAmount) {
        ParkingSession session = requireSession(sessionId);
        if (session.getStatus() == ParkingSessionStatus.VOIDED) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        BigDecimal next = session.paidAmountOrZero().subtract(refundAmount);
        if (next.signum() < 0) {
            next = BigDecimal.ZERO;
        }
        session.setPaidAmountYuan(next);
        if (session.getStatus() == ParkingSessionStatus.CLOSED) {
            if (next.signum() <= 0) {
                session.setPayStatus(ParkingPayStatus.UNPAID);
                session.setPayTime(null);
            } else {
                session.syncPayStatusFromMoney();
            }
        }
        return saveAndPush(session);
    }

    /**
     * 下单前可收款预览（供停车订单创建/前端确认弹窗调用）：返回当前应收口径、
     * 该流水累计已支付、该流水待付订单合计与本次可收款金额。
     * 本次可收款 = 应收 − 累计已支付 − 待付订单，避免在场多次缴费重复计费。
     */
    @Transactional(readOnly = true)
    public PayableQuoteView payableQuote(Long sessionId) {
        ParkingSession session = requireSession(sessionId);
        if (session.getStatus() == ParkingSessionStatus.VOIDED) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        BigDecimal receivable = currentReceivable(session);
        BigDecimal receivableSafe = receivable == null ? BigDecimal.ZERO : receivable;
        BigDecimal paid = session.paidAmountOrZero();
        BigDecimal pending = pendingOrdersAmounts(toSessionIds(List.of(session)))
                .getOrDefault(sessionId, BigDecimal.ZERO);
        BigDecimal payable = maxZero(receivableSafe.subtract(paid).subtract(pending));
        return new PayableQuoteView(sessionId, receivableSafe, paid, pending, payable);
    }

    /**
     * 批量汇总若干流水「未支付/待支付」订单金额：未命中流水的 key 不存在（调用方用默认 0）。
     */
    private Map<Long, BigDecimal> pendingOrdersAmounts(Collection<Long> sessionIds) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (sessionIds == null || sessionIds.isEmpty()) {
            return result;
        }
        List<Long> ids = sessionIds.stream().distinct().toList();
        for (Object[] row : orders.sumAmountGroupBySessionId(ids, ParkingOrderStatus.PENDING)) {
            result.put((Long) row[0], (BigDecimal) row[1]);
        }
        return result;
    }

    /**
     * 批量汇总若干流水「管理端人工下单」的待付订单金额（{@code paymentNo} 为空）：
     * C 端在线支付自身拆出的待付订单不计入，避免用户中途放弃在线支付导致查费金额凭空减少。
     */
    private Map<Long, BigDecimal> manualPendingOrdersAmounts(Collection<Long> sessionIds) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (sessionIds == null || sessionIds.isEmpty()) {
            return result;
        }
        List<Long> ids = sessionIds.stream().distinct().toList();
        for (Object[] row : orders.sumManualAmountGroupBySessionId(ids, ParkingOrderStatus.PENDING)) {
            result.put((Long) row[0], (BigDecimal) row[1]);
        }
        return result;
    }

    /** 历史已登记支付（无金额记录）：已出场、支付状态为已支付、但累计已支付为 0 的单据，不再计入 C 端欠费展示。 */
    private static boolean isHistoricallySettled(ParkingSession session) {
        return session.getPayStatus() == ParkingPayStatus.PAID
                && session.paidAmountOrZero().signum() == 0;
    }

    private static List<Long> toSessionIds(Collection<ParkingSession> sessions) {
        List<Long> ids = new ArrayList<>(sessions.size());
        for (ParkingSession session : sessions) {
            ids.add(session.getId());
        }
        return ids;
    }

    /** 金额非负化：负数取 0（应付不会为负）。 */
    private static BigDecimal maxZero(BigDecimal value) {
        return value == null || value.signum() <= 0 ? BigDecimal.ZERO : value;
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

    /**
     * 云端写流水：修订号 +1 后落库，事务提交后经 MQTT 下发到绑定的边缘节点。
     */
    private ParkingSession saveAndPush(ParkingSession session) {
        long next = session.getCloudRevision() == null ? 1L : session.getCloudRevision() + 1;
        session.setCloudRevision(next);
        bindEdgeNode(session);
        ParkingSession saved = sessions.save(session);
        events.publishEvent(new ParkingSessionChangedEvent(saved.getId()));
        return saved;
    }

    private void bindEdgeNode(ParkingSession session, ParkingLot lot) {
        if (session.getEdgeNodeCode() == null && lot != null && StringUtils.hasText(lot.getEdgeNodeCode())) {
            session.setEdgeNodeCode(lot.getEdgeNodeCode());
        }
    }

    private void bindEdgeNode(ParkingSession session) {
        if (session.getEdgeNodeCode() != null || session.getLotId() == null) {
            return;
        }
        lots.findById(session.getLotId()).ifPresent(lot -> bindEdgeNode(session, lot));
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

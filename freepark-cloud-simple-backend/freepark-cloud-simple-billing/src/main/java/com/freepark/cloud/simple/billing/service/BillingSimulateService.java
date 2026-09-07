package com.freepark.cloud.simple.billing.service;

import com.freepark.cloud.simple.billing.dto.BillingSimulateRequest;
import com.freepark.cloud.simple.billing.dto.BillingSimulateResult;
import com.freepark.cloud.simple.billing.entity.BillingCycleProfile;
import com.freepark.cloud.simple.billing.entity.BillingCycleSegment;
import com.freepark.cloud.simple.billing.entity.BillingDailyRule;
import com.freepark.cloud.simple.billing.entity.BillingGeneralRule;
import com.freepark.cloud.simple.billing.entity.BillingSpecialDate;
import com.freepark.cloud.simple.billing.entity.CycleTailMode;
import com.freepark.cloud.simple.billing.entity.SpecialDateType;
import com.freepark.cloud.simple.billing.repository.BillingCycleProfileRepository;
import com.freepark.cloud.simple.billing.repository.BillingCycleSegmentRepository;
import com.freepark.cloud.simple.billing.repository.BillingDailyRuleRepository;
import com.freepark.cloud.simple.billing.repository.BillingDailySlotRepository;
import com.freepark.cloud.simple.billing.repository.BillingGeneralRuleRepository;
import com.freepark.cloud.simple.billing.repository.BillingGeneralSlotRepository;
import com.freepark.cloud.simple.billing.repository.BillingSpecialDateRepository;
import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.time.SiteZoneProvider;
import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * 模拟算费引擎（只读）：对一段「入场 → 出场」的连续停车，按某条计费模板试算应收金额。
 * <p>
 * 模板分两类，结算口径不同：
 * <ul>
 *   <li><b>每日制（{@link BillingDailyRule}）</b>：按自然日逐日独立结算。某日车辆存在可计费
 *       时长时才参与：先扣减一次 {@code freeMinutes} 免费时长，再按固定周期 / 计费周期方案累计，
 *       当日费用以「每日封顶」为上限（0 = 不封顶）。周计划口径：当天未配置 = 全天可计费；
 *       配置收费时段 = 仅时段内可计费；标记「当天免费」= 当天不参与。</li>
 *   <li><b>24 小时制（{@link BillingGeneralRule}）</b>：以入场时刻为起点、每连续 24 小时为一个
 *       计费周期窗口。每个窗口先扣减一次 {@code freeMinutes} 免费时长，再按固定周期 /
 *       计费周期方案累计（分段费率在每个窗口开头重新展开），窗口费用以「每 24 小时封顶」为上限。
 *       某自然日是否收费先做整日判定：命中节假日免费（{@code holidayFree} 且当天落在全局节假日区间）
 *       或周六日免费（{@code weekendFree} 且当天为周六/周日，补班日覆盖周末免费）时整天不收费；
 *       未命中的自然日再按周计划逐天细化。</li>
 * </ul>
 * 免费日 / 免费时段不消耗免费时长，也不推进分段档位；「再次计费时长」（场内缴费后离场宽限）只对
 * 「缴费后又继续停放」的事件生效，一次连续停车（无场内缴费事件）的模拟中不产生该费用，故按 0 处理。
 * </p>
 * <p>
 * 时间语义与全系统一致：请求区间与全局特殊日期在库内均为 UTC 挂钟锚点，引擎先换算到系统配置时区的
 * 本地挂钟时间，再按本地自然日做逐日 / 逐窗口结算（金额与时间边界展示口径一致）。
 * </p>
 */
@Service
public class BillingSimulateService {

    /** 每 24 小时分钟数 */
    private static final int MINUTES_PER_DAY = 1440;

    /** 模拟区间上限：366 天（约一年），避免极端输入拖垮引擎 */
    private static final long MAX_INTERVAL_DAYS = 366;

    private final BillingGeneralRuleRepository generalRules;
    private final BillingGeneralSlotRepository generalSlots;
    private final BillingDailyRuleRepository dailyRules;
    private final BillingDailySlotRepository dailySlots;
    private final BillingCycleProfileRepository profiles;
    private final BillingCycleSegmentRepository segments;
    private final BillingSpecialDateRepository specialDates;
    private final SiteZoneProvider zoneProvider;

    public BillingSimulateService(BillingGeneralRuleRepository generalRules,
                                  BillingGeneralSlotRepository generalSlots,
                                  BillingDailyRuleRepository dailyRules,
                                  BillingDailySlotRepository dailySlots,
                                  BillingCycleProfileRepository profiles,
                                  BillingCycleSegmentRepository segments,
                                  BillingSpecialDateRepository specialDates,
                                  SiteZoneProvider zoneProvider) {
        this.generalRules = generalRules;
        this.generalSlots = generalSlots;
        this.dailyRules = dailyRules;
        this.dailySlots = dailySlots;
        this.profiles = profiles;
        this.segments = segments;
        this.specialDates = specialDates;
        this.zoneProvider = zoneProvider;
    }

    /** 每日制规则模拟算费。 */
    @Transactional(readOnly = true)
    public BillingSimulateResult simulateDaily(Long ruleId, BillingSimulateRequest request) {
        BillingDailyRule rule = dailyRules.findById(ruleId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
        Interval interval = requireInterval(request);

        Map<Integer, DayPlan> plans = buildDailyPlans(rule.getId());

        BigDecimal total = BigDecimal.ZERO;
        LocalDate date = interval.startWall().toLocalDate();
        LocalDate lastDate = interval.endWall().toLocalDate();
        while (!date.isAfter(lastDate)) {
            DayPlan plan = plans.getOrDefault(date.getDayOfWeek().getValue(), DayPlan.allDay());
            if (!plan.free()) {
                long chargeable = chargeableMinutesOfDay(plan, date, interval.startWall(), interval.endWall());
                if (chargeable > 0) {
                    long feeable = Math.max(0L, chargeable - rule.getFreeMinutes());
                    BigDecimal dayFee = charge(rule.getCycleMinutes(), rule.getUnitPriceYuan(),
                            rule.getCycleProfileId(), feeable);
                    total = total.add(cap(dayFee, rule.getCapPerDayYuan()));
                }
            }
            date = date.plusDays(1);
        }
        return new BillingSimulateResult(total.setScale(2, RoundingMode.HALF_UP));
    }

    /** 24 小时制规则模拟算费。 */
    @Transactional(readOnly = true)
    public BillingSimulateResult simulateGeneral(Long ruleId, BillingSimulateRequest request) {
        BillingGeneralRule rule = generalRules.findById(ruleId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
        Interval interval = requireInterval(request);

        Map<Integer, DayPlan> plans = buildGeneralPlans(rule.getId());
        List<DayGate> gates = buildDayGates(rule);

        // 按 24 小时窗口汇总各窗口内的可计费分钟数
        Map<Long, Long> roundMinutes = new HashMap<>();
        LocalDate date = interval.startWall().toLocalDate();
        LocalDate lastDate = interval.endWall().toLocalDate();
        while (!date.isAfter(lastDate)) {
            if (!isDayFree(rule, gates, date)) {
                DayPlan plan = plans.getOrDefault(date.getDayOfWeek().getValue(), DayPlan.allDay());
                if (!plan.free()) {
                    // 当天每个收费窗口内与停车区间相交的一段，按 24h 窗口边界切分累计
                    LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
                    for (long[] window : plan.windows()) {
                        long from = Math.max(window[0], minutesBetween(dayStart, interval.startWall()));
                        long to = Math.min(window[1], minutesBetween(dayStart, interval.endWall()));
                        if (to > from) {
                            accrueRoundMinutes(roundMinutes, interval.startWall(), dayStart.plusMinutes(from),
                                    dayStart.plusMinutes(to));
                        }
                    }
                }
            }
            date = date.plusDays(1);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (long minutes : roundMinutes.values()) {
            long feeable = Math.max(0L, minutes - rule.getFreeMinutes());
            BigDecimal roundFee = charge(rule.getCycleMinutes(), rule.getUnitPriceYuan(),
                    rule.getCycleProfileId(), feeable);
            total = total.add(cap(roundFee, rule.getCapPer24hYuan()));
        }
        return new BillingSimulateResult(total.setScale(2, RoundingMode.HALF_UP));
    }

    /** 校验并规范化模拟区间（换到站点本地挂钟时间，边界截断到分钟）。 */
    private Interval requireInterval(BillingSimulateRequest request) {
        if (request == null || request.startTime() == null || request.endTime() == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        ZoneId zone = zoneProvider.currentZone();
        LocalDateTime start = SiteZoneTimes.toSiteWall(request.startTime(), zone).truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime end = SiteZoneTimes.toSiteWall(request.endTime(), zone).truncatedTo(ChronoUnit.MINUTES);
        if (!start.isBefore(end)) {
            throw new BizException(400, MessageKeys.BILLING_SIMULATE_INVALID_RANGE);
        }
        if (ChronoUnit.DAYS.between(start, end) >= MAX_INTERVAL_DAYS + 1) {
            throw new BizException(400, MessageKeys.BILLING_SIMULATE_TOO_LONG);
        }
        return new Interval(start, end);
    }

    // ---- 周计划 ----

    private record DayPlan(boolean free, List<long[]> windows) {

        /** 未配置任何行的默认口径：全天可计费。 */
        static DayPlan allDay() {
            return new DayPlan(false, List.of(new long[]{0, MINUTES_PER_DAY}));
        }
    }

    private Map<Integer, DayPlan> buildDailyPlans(Long ruleId) {
        return buildPlans(dailySlots.findByRuleIdOrderByWeekdayAscStartMinuteAscIdAsc(ruleId)
                .stream()
                .map(slot -> new SlotRow(slot.getWeekday(), slot.isAllDayFree(),
                        slot.getStartMinute(), slot.getEndMinute()))
                .toList());
    }

    private Map<Integer, DayPlan> buildGeneralPlans(Long ruleId) {
        return buildPlans(generalSlots.findByRuleIdOrderByWeekdayAscStartMinuteAscIdAsc(ruleId)
                .stream()
                .map(slot -> new SlotRow(slot.getWeekday(), slot.isAllDayFree(),
                        slot.getStartMinute(), slot.getEndMinute()))
                .toList());
    }

    private record SlotRow(int weekday, boolean allDayFree, Integer startMinute, Integer endMinute) {
    }

    /** 将周计划行聚合为 {weekday → 当天计划}；未出现的星期按全天可计费处理。 */
    private Map<Integer, DayPlan> buildPlans(List<SlotRow> rows) {
        Map<Integer, DayPlan> plans = new HashMap<>();
        for (Integer weekday : new Integer[]{1, 2, 3, 4, 5, 6, 7}) {
            List<SlotRow> dayRows = rows.stream()
                    .filter(row -> row.weekday() == weekday)
                    .sorted(Comparator.comparingInt(SlotRow::startMinute))
                    .toList();
            if (dayRows.isEmpty()) {
                continue; // 未配置 = 全天可计费
            }
            if (dayRows.stream().anyMatch(SlotRow::allDayFree)) {
                plans.put(weekday, new DayPlan(true, List.of()));
            } else {
                List<long[]> windows = new ArrayList<>();
                for (SlotRow row : dayRows) {
                    windows.add(new long[]{row.startMinute(), row.endMinute()});
                }
                plans.put(weekday, new DayPlan(false, windows));
            }
        }
        return plans;
    }

    /**
     * 停车区间在某自然日某天的可计费分钟数：区间 ∩ 当天收费窗口 的总分钟数。
     * 分钟计数只取整天、整分钟输入，故交点长度天然为整数分钟。
     */
    private long chargeableMinutesOfDay(DayPlan plan, LocalDate date,
                                        LocalDateTime startWall, LocalDateTime endWall) {
        long total = 0;
        LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
        for (long[] window : plan.windows()) {
            long from = Math.max(window[0], minutesBetween(dayStart, startWall));
            long to = Math.min(window[1], minutesBetween(dayStart, endWall));
            if (to > from) {
                total += to - from;
            }
        }
        return total;
    }

    /** 时刻相对某天 0 点的分钟偏移；早于当天为负、晚于当天为超大的正数，由调用方 clamp。 */
    private long minutesBetween(LocalDateTime dayStart, LocalDateTime wall) {
        return ChronoUnit.MINUTES.between(dayStart, wall);
    }

    /** 把一段可计费时间按「入场时刻起 24 小时窗口」切分并累加进各窗口。 */
    private void accrueRoundMinutes(Map<Long, Long> roundMinutes,
                                    LocalDateTime anchor, LocalDateTime from, LocalDateTime to) {
        LocalDateTime cursor = from;
        while (cursor.isBefore(to)) {
            long offset = ChronoUnit.MINUTES.between(anchor, cursor);
            long roundIndex = floorDiv(offset, MINUTES_PER_DAY);
            LocalDateTime roundEnd = anchor.plusMinutes((roundIndex + 1) * (long) MINUTES_PER_DAY);
            LocalDateTime end = to.isBefore(roundEnd) ? to : roundEnd;
            long minutes = ChronoUnit.MINUTES.between(cursor, end);
            roundMinutes.merge(roundIndex, minutes, Long::sum);
            cursor = end;
        }
    }

    private static long floorDiv(long a, long b) {
        return Math.floorDiv(a, b);
    }

    // ---- 24 小时制整日免费判定（节假日 / 周六日 / 补班） ----

    /** 单条特殊日期区间的站点本地化视图。 */
    private record DayGate(SpecialDateType type, LocalDateTime startWall, LocalDateTime endWall) {

        /** 该自然日是否落在区间内（[start, end) 与当天 [00:00, 24:00) 相交）。 */
        boolean covers(LocalDate date) {
            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = dayStart.plusDays(1);
            return startWall.isBefore(dayEnd) && dayStart.isBefore(endWall);
        }
    }

    private List<DayGate> buildDayGates(BillingGeneralRule rule) {
        List<DayGate> gates = new ArrayList<>();
        if (!rule.isHolidayFree() && !rule.isWeekendFree()) {
            return gates;
        }
        ZoneId zone = zoneProvider.currentZone();
        for (BillingSpecialDate entry : specialDates.findAllByOrderByStartTimeAscEndTimeAsc()) {
            SpecialDateType type = entry.getType();
            if (type == SpecialDateType.HOLIDAY && !rule.isHolidayFree()) {
                continue;
            }
            if (type == SpecialDateType.MAKEUP_WORKDAY && !rule.isWeekendFree()) {
                continue; // 补班只用于覆盖“周六日免费”
            }
            gates.add(new DayGate(type, SiteZoneTimes.toSiteWall(entry.getStartTime(), zone),
                    SiteZoneTimes.toSiteWall(entry.getEndTime(), zone)));
        }
        return gates;
    }

    /**
     * 24 小时制整日免费判定：优先节假日免费（命中即整天不收费）；
     * 其次周六日免费（当天为周六/周日时整天不收费，补班日覆盖周末免费）；
     * 其余情况按周计划逐天收费。
     */
    private boolean isDayFree(BillingGeneralRule rule, List<DayGate> gates, LocalDate date) {
        if (rule.isHolidayFree()) {
            for (DayGate gate : gates) {
                if (gate.type() == SpecialDateType.HOLIDAY && gate.covers(date)) {
                    return true;
                }
            }
        }
        if (rule.isWeekendFree()) {
            DayOfWeek dow = date.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                for (DayGate gate : gates) {
                    if (gate.type() == SpecialDateType.MAKEUP_WORKDAY && gate.covers(date)) {
                        return false; // 补班日调为工作日
                    }
                }
                return true;
            }
        }
        return false;
    }

    // ---- 计价 ----

    /** 对一段净计费时长（已扣免费时长）按固定周期或分段费率方案计费。 */
    private BigDecimal charge(int cycleMinutes, BigDecimal unitPriceYuan, Long cycleProfileId, long feeableMinutes) {
        if (feeableMinutes <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (cycleProfileId == null) {
            long cycles = (feeableMinutes + cycleMinutes - 1) / cycleMinutes; // 不足一个整档按整档
            return unitPriceYuan.multiply(BigDecimal.valueOf(cycles))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return chargeByProfile(cycleProfileId, feeableMinutes);
    }

    /** 按计费周期方案的分段费率逐段累计；全部档位用尽后超出的时长不再计费。 */
    private BigDecimal chargeByProfile(Long profileId, long feeableMinutes) {
        BillingCycleProfile profile = profiles.findById(profileId).orElse(null);
        CycleTailMode tailMode = profile == null ? CycleTailMode.NO_CHARGE : parseTailMode(profile.getTailMode());
        List<BillingCycleSegment> rows = segments.findByProfileIdOrderBySeqAsc(profileId);

        BigDecimal fee = BigDecimal.ZERO;
        long remaining = feeableMinutes;
        for (BillingCycleSegment row : rows) {
            if (remaining <= 0) {
                break;
            }
            int segmentMinutes = row.getMinutes();
            BigDecimal price = row.getUnitPriceYuan();
            int repeat = Math.max(row.getRepeatCount(), 1);
            for (int i = 0; i < repeat; i++) {
                if (remaining <= 0) {
                    break;
                }
                if (remaining >= segmentMinutes) {
                    fee = fee.add(price);
                    remaining -= segmentMinutes;
                } else {
                    // 不足一个完整分段的尾数时长，按方案尾数方式计价
                    fee = fee.add(tailFee(segmentMinutes, price, remaining, tailMode));
                    remaining = 0;
                }
            }
        }
        return fee.setScale(2, RoundingMode.HALF_UP);
    }

    private CycleTailMode parseTailMode(String raw) {
        if (raw != null) {
            for (CycleTailMode candidate : CycleTailMode.values()) {
                if (candidate.name().equalsIgnoreCase(raw.trim())) {
                    return candidate;
                }
            }
        }
        return CycleTailMode.NO_CHARGE;
    }

    /** 尾数不足一个完整分段时，按方案的尾数方式折算金额。 */
    private BigDecimal tailFee(int segmentMinutes, BigDecimal price, long partialMinutes, CycleTailMode tailMode) {
        return switch (tailMode) {
            case WHOLE_SEGMENT -> price;
            case PROPORTIONAL -> price.multiply(BigDecimal.valueOf(partialMinutes))
                    .divide(BigDecimal.valueOf(segmentMinutes), 2, RoundingMode.HALF_UP);
            case NO_CHARGE -> BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        };
    }

    /** 封顶：cap > 0 时费用不超过 cap。 */
    private BigDecimal cap(BigDecimal fee, BigDecimal capYuan) {
        if (capYuan == null || capYuan.signum() <= 0 || fee.compareTo(capYuan) <= 0) {
            return fee;
        }
        return capYuan;
    }

    /** 模拟区间（已换算为站点本地挂钟时间，分钟级）。 */
    private record Interval(LocalDateTime startWall, LocalDateTime endWall) {
    }
}

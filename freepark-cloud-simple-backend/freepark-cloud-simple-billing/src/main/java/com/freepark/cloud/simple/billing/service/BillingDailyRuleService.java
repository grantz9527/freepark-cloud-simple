package com.freepark.cloud.simple.billing.service;

import com.freepark.cloud.simple.billing.dto.BillingDailyRuleRequest;
import com.freepark.cloud.simple.billing.dto.BillingDailyRuleView;
import com.freepark.cloud.simple.billing.dto.BillingDailySlotRequest;
import com.freepark.cloud.simple.billing.dto.BillingDailySlotView;
import com.freepark.cloud.simple.billing.entity.BillingDailyRule;
import com.freepark.cloud.simple.billing.entity.BillingDailySlot;
import com.freepark.cloud.simple.billing.entity.BillingLotBinding;
import com.freepark.cloud.simple.billing.repository.BillingCycleProfileRepository;
import com.freepark.cloud.simple.billing.repository.BillingDailyRuleRepository;
import com.freepark.cloud.simple.billing.repository.BillingDailySlotRepository;
import com.freepark.cloud.simple.billing.repository.BillingLotBindingRepository;
import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import com.freepark.cloud.simple.user.service.AdminGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 每日制计费规则（全局模板）服务。
 * <p>
 * 规则模板不直接归属车场或车牌颜色，同一条模板可被多个车场在「计费配置」中引用；
 * 模板被任一车场计费配置引用后不可删除（见 {@link BillingLotBinding}）。
 * 周计划口径：某星期未配置任何信息 = 当天全天收费；配置了收费时段 = 仅该时段收费；
 * 标记「当天免费」= 整天不收费（免费行与收费时段不得同时出现在同一天）。
 * </p>
 */
@Service
public class BillingDailyRuleService {

    /** 全天最大分钟数 */
    private static final int MAX_DAY_MINUTES = 1440;

    /** 免费/宽限/单档时长上限（与 24 小时制规则一致：不超过 24 小时） */
    private static final int MAX_MINUTES = MAX_DAY_MINUTES;

    /** 单个金额上限（避免异常配置） */
    private static final BigDecimal MAX_MONEY = new BigDecimal("99999999.99");

    /** 周计划提交行数上限（7 天，每天若干时段 + 免费标记） */
    private static final int MAX_SLOTS = 64;

    private static final int MAX_TITLE_LENGTH = 80;
    private static final int MAX_DESCRIPTION_LENGTH = 255;

    private final BillingDailyRuleRepository rules;
    private final BillingDailySlotRepository slots;
    private final BillingCycleProfileRepository profiles;
    private final BillingLotBindingRepository bindings;
    private final AdminGuard adminGuard;

    public BillingDailyRuleService(BillingDailyRuleRepository rules,
                                   BillingDailySlotRepository slots,
                                   BillingCycleProfileRepository profiles,
                                   BillingLotBindingRepository bindings,
                                   AdminGuard adminGuard) {
        this.rules = rules;
        this.slots = slots;
        this.profiles = profiles;
        this.bindings = bindings;
        this.adminGuard = adminGuard;
    }

    @Transactional(readOnly = true)
    public List<BillingDailyRuleView> list() {
        return rules.findAllByOrderByIdAsc().stream()
                .map(this::toView)
                .toList();
    }

    @Transactional
    public BillingDailyRuleView create(BillingDailyRuleRequest request) {
        adminGuard.requireEnabledAdmin();
        BillingDailyRuleRequest req = requireRequest(request);
        RuleInput input = normalize(req);

        BillingDailyRule rule = new BillingDailyRule();
        applyRule(rule, input);
        LocalDateTime now = SiteZoneTimes.nowUtc();
        rule.setCreatedAt(now);
        rule.setUpdatedAt(now);
        rule = rules.save(rule);
        saveSlots(rule.getId(), input.slots());
        return toView(rule);
    }

    @Transactional
    public BillingDailyRuleView update(Long ruleId, BillingDailyRuleRequest request) {
        adminGuard.requireEnabledAdmin();
        BillingDailyRule rule = requireRule(ruleId);
        RuleInput input = normalize(requireRequest(request));

        applyRule(rule, input);
        rule.setUpdatedAt(SiteZoneTimes.nowUtc());
        rules.save(rule);
        // 周计划整组替换，避免残留过期行
        slots.deleteByRuleId(rule.getId());
        saveSlots(rule.getId(), input.slots());
        return toView(rule);
    }

    @Transactional
    public void delete(Long ruleId) {
        adminGuard.requireEnabledAdmin();
        BillingDailyRule rule = requireRule(ruleId);
        if (!bindings.findByRuleTypeAndRuleId(BillingLotBinding.TYPE_DAILY, rule.getId()).isEmpty()) {
            throw new BizException(400, MessageKeys.BILLING_RULE_REFERENCED);
        }
        slots.deleteByRuleId(rule.getId());
        rules.delete(rule);
    }

    private void applyRule(BillingDailyRule rule, RuleInput input) {
        rule.setTitle(input.title());
        rule.setDescription(input.description());
        rule.setFreeMinutes(input.freeMinutes());
        rule.setGraceMinutes(input.graceMinutes());
        rule.setCycleMinutes(input.cycleMinutes());
        rule.setUnitPriceYuan(input.unitPriceYuan());
        rule.setCapPerDayYuan(input.capPerDayYuan());
        rule.setCycleProfileId(input.cycleProfileId());
    }

    private void saveSlots(Long ruleId, List<SlotInput> items) {
        for (SlotInput item : items) {
            BillingDailySlot slot = new BillingDailySlot();
            slot.setRuleId(ruleId);
            slot.setWeekday(item.weekday());
            slot.setAllDayFree(item.allDayFree());
            if (!item.allDayFree()) {
                slot.setStartMinute(item.startMinute());
                slot.setEndMinute(item.endMinute());
            }
            slots.save(slot);
        }
    }

    private BillingDailyRuleRequest requireRequest(BillingDailyRuleRequest request) {
        if (request == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return request;
    }

    private record RuleInput(String title, String description,
                             int freeMinutes, int graceMinutes, int cycleMinutes,
                             BigDecimal unitPriceYuan, BigDecimal capPerDayYuan, Long cycleProfileId,
                             List<SlotInput> slots) {
    }

    private record SlotInput(int weekday, boolean allDayFree, Integer startMinute, Integer endMinute) {
    }

    private RuleInput normalize(BillingDailyRuleRequest request) {
        String title = normalizeTitle(request.title());
        String description = normalizeDescription(request.description());
        int freeMinutes = requireMinutes(request.freeMinutes(), "freeMinutes");
        int graceMinutes = requireMinutes(request.graceMinutes(), "graceMinutes");
        int cycleMinutes = requireCycleMinutes(request.cycleMinutes());
        BigDecimal unitPrice = normalizeMoney(request.unitPriceYuan());
        BigDecimal cap = normalizeMoney(request.capPerDayYuan());
        Long cycleProfileId = normalizeProfileId(request.cycleProfileId());
        List<SlotInput> slotInputs = normalizeSlots(request.slots());
        return new RuleInput(title, description, freeMinutes, graceMinutes,
                cycleMinutes, unitPrice, cap, cycleProfileId, slotInputs);
    }

    /**
     * 校验并规范化周计划：星期取值、时段边界/长度、同天不重叠、免费与时段不冲突。
     */
    private List<SlotInput> normalizeSlots(List<BillingDailySlotRequest> items) {
        List<SlotInput> result = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            return result;
        }
        if (items.size() > MAX_SLOTS) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        // 按星期聚合，用于跨行的重叠 / 冲突校验
        Map<Integer, List<SlotInput>> byWeekday = new HashMap<>();
        for (BillingDailySlotRequest item : items) {
            if (item == null) {
                throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
            }
            int weekday = item.weekday();
            if (weekday < 1 || weekday > 7) {
                throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
            }
            SlotInput slot;
            if (item.allDayFree()) {
                // 当天整体免费：忽略时段
                slot = new SlotInput(weekday, true, null, null);
            } else {
                Integer start = item.startMinute();
                Integer end = item.endMinute();
                if (start == null || end == null || start < 0 || start >= MAX_DAY_MINUTES
                        || end <= start || end > MAX_DAY_MINUTES) {
                    throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
                }
                slot = new SlotInput(weekday, false, start, end);
            }
            result.add(slot);
            byWeekday.computeIfAbsent(weekday, k -> new ArrayList<>()).add(slot);
        }
        // 逐天校验：免费日独占；收费时段不重叠（相邻时段允许，[start,end) 左闭右开）
        for (Map.Entry<Integer, List<SlotInput>> entry : byWeekday.entrySet()) {
            List<SlotInput> daySlots = entry.getValue();
            boolean hasFreeDay = false;
            List<SlotInput> windows = new ArrayList<>();
            for (SlotInput slot : daySlots) {
                if (slot.allDayFree()) {
                    if (hasFreeDay) {
                        throw new BizException(400, MessageKeys.BILLING_DAILY_SLOT_FREE_CONFLICT);
                    }
                    hasFreeDay = true;
                } else {
                    windows.add(slot);
                }
            }
            if (hasFreeDay && !windows.isEmpty()) {
                throw new BizException(400, MessageKeys.BILLING_DAILY_SLOT_FREE_CONFLICT);
            }
            windows.sort(Comparator.comparingInt(SlotInput::startMinute));
            int previousEnd = -1;
            for (SlotInput window : windows) {
                if (previousEnd > window.startMinute()) {
                    throw new BizException(400, MessageKeys.BILLING_DAILY_SLOT_OVERLAP);
                }
                previousEnd = window.endMinute();
            }
        }
        // 按星期再按时间稳定排序，保持输出一致
        result.sort(Comparator.comparingInt(SlotInput::weekday)
                .thenComparing(slot -> slot.allDayFree() ? -1 : slot.startMinute()));
        return result;
    }

    private String normalizeTitle(String value) {
        String title = value == null ? null : value.trim();
        if (!StringUtils.hasText(title) || title.length() > MAX_TITLE_LENGTH) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return title;
    }

    private String normalizeDescription(String value) {
        String description = value == null ? null : value.trim();
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return StringUtils.hasText(description) ? description : null;
    }

    private int requireMinutes(int value, String field) {
        if (value < 0 || value > MAX_MINUTES) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return value;
    }

    private int requireCycleMinutes(int value) {
        if (value < 1 || value > MAX_MINUTES) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return value;
    }

    private BigDecimal normalizeMoney(BigDecimal raw) {
        BigDecimal money = raw == null ? BigDecimal.ZERO : raw;
        if (money.signum() < 0 || money.compareTo(MAX_MONEY) > 0) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        try {
            return money.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            // 超过两位小数
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
    }

    /**
     * 选用的计费周期方案必须真实存在；为空表示固定周期计费。
     */
    private Long normalizeProfileId(Long profileId) {
        if (profileId == null) {
            return null;
        }
        if (profileId <= 0 || !profiles.existsById(profileId)) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return profileId;
    }

    private BillingDailyRule requireRule(Long ruleId) {
        return rules.findById(ruleId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
    }

    private BillingDailyRuleView toView(BillingDailyRule rule) {
        List<BillingDailySlotView> slotViews = slots.findByRuleIdOrderByWeekdayAscStartMinuteAscIdAsc(rule.getId())
                .stream()
                .map(slot -> new BillingDailySlotView(
                        slot.getId(),
                        slot.getWeekday(),
                        slot.isAllDayFree(),
                        slot.getStartMinute(),
                        slot.getEndMinute()))
                .toList();
        return new BillingDailyRuleView(
                rule.getId(),
                rule.getTitle(),
                rule.getDescription(),
                rule.getFreeMinutes(),
                rule.getGraceMinutes(),
                rule.getCycleMinutes(),
                rule.getUnitPriceYuan(),
                rule.getCapPerDayYuan(),
                rule.getCycleProfileId(),
                slotViews,
                rule.getCreatedAt(),
                rule.getUpdatedAt());
    }
}

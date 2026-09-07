package com.freepark.cloud.simple.billing.service;

import com.freepark.cloud.simple.billing.dto.BillingLotBindingRequest;
import com.freepark.cloud.simple.billing.dto.BillingLotBindingView;
import com.freepark.cloud.simple.billing.entity.BillingLotBinding;
import com.freepark.cloud.simple.billing.repository.BillingDailyRuleRepository;
import com.freepark.cloud.simple.billing.repository.BillingGeneralRuleRepository;
import com.freepark.cloud.simple.billing.repository.BillingLotBindingRepository;
import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import com.freepark.cloud.simple.user.service.AdminGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 车场计费配置服务：把「每日制/24 小时制」全局规则模板手动绑定到具体车场，
 * 每条配置含车牌颜色（空 = 默认）与生效起止日期，供云端算费引擎按车场取用。
 * <p>
 * 冲突约束：同一车场、同一车牌颜色（空视为同一「默认」）的配置生效区间不可重叠，
 * 跨每日制 / 24 小时制同样拦截，避免同一车场对同一颜色在同一时段存在两套计费口径；
 * 不同车牌颜色（含默认与专属颜色并存）可同时生效，专属颜色优先于默认。
 * </p>
 */
@Service
public class BillingLotBindingService {

    private static final int MAX_COLOR_LENGTH = 32;

    private final BillingLotBindingRepository bindings;
    private final BillingDailyRuleRepository dailyRules;
    private final BillingGeneralRuleRepository generalRules;
    private final AdminGuard adminGuard;

    public BillingLotBindingService(BillingLotBindingRepository bindings,
                                    BillingDailyRuleRepository dailyRules,
                                    BillingGeneralRuleRepository generalRules,
                                    AdminGuard adminGuard) {
        this.bindings = bindings;
        this.dailyRules = dailyRules;
        this.generalRules = generalRules;
        this.adminGuard = adminGuard;
    }

    /** 按车场查询；省略 lotId 返回全部（供模板页反查引用等场景）。 */
    @Transactional(readOnly = true)
    public List<BillingLotBindingView> list(Long lotId) {
        List<BillingLotBinding> entries = lotId == null
                ? bindings.findAllByOrderByLotIdAscEffectiveFromAscIdAsc()
                : bindings.findByLotIdOrderByEffectiveFromAscIdAsc(lotId);
        Comparator<BillingLotBinding> ordered =
                Comparator.comparing(BillingLotBinding::getLotId)
                        .thenComparing(binding -> binding.getPlateColor() == null ? 0 : 1)
                        .thenComparing(binding -> binding.getPlateColor() == null ? "" : binding.getPlateColor())
                        .thenComparing(binding -> binding.getEffectiveFrom() == null
                                ? LocalDate.MIN : binding.getEffectiveFrom())
                        .thenComparing(BillingLotBinding::getId);
        return entries.stream()
                .sorted(ordered)
                .map(this::toView)
                .toList();
    }

    @Transactional
    public BillingLotBindingView create(BillingLotBindingRequest request) {
        adminGuard.requireEnabledAdmin();
        LotBindingInput input = normalize(request);

        ensureNoOverlap(input, null);

        BillingLotBinding binding = new BillingLotBinding();
        apply(binding, input);
        LocalDateTime now = SiteZoneTimes.nowUtc();
        binding.setCreatedAt(now);
        binding.setUpdatedAt(now);
        return toView(bindings.save(binding));
    }

    @Transactional
    public BillingLotBindingView update(Long bindingId, BillingLotBindingRequest request) {
        adminGuard.requireEnabledAdmin();
        BillingLotBinding binding = requireBinding(bindingId);
        LotBindingInput input = normalize(request);

        ensureNoOverlap(input, binding.getId());

        apply(binding, input);
        binding.setUpdatedAt(SiteZoneTimes.nowUtc());
        return toView(bindings.save(binding));
    }

    @Transactional
    public void delete(Long bindingId) {
        adminGuard.requireEnabledAdmin();
        bindings.delete(requireBinding(bindingId));
    }

    private void apply(BillingLotBinding binding, LotBindingInput input) {
        binding.setLotId(input.lotId());
        binding.setRuleType(input.ruleType());
        binding.setRuleId(input.ruleId());
        binding.setPlateColor(input.plateColor());
        binding.setEffectiveFrom(input.effectiveFrom());
        binding.setEffectiveTo(input.effectiveTo());
    }

    private void ensureNoOverlap(LotBindingInput input, Long excludeId) {
        List<BillingLotBinding> sameLot = bindings.findByLotId(input.lotId());
        for (BillingLotBinding existing : sameLot) {
            if (excludeId != null && existing.getId().equals(excludeId)) {
                continue;
            }
            if (!sameColor(input.plateColor(), existing.getPlateColor())) {
                continue;
            }
            if (rangesOverlap(input.effectiveFrom(), input.effectiveTo(),
                    existing.getEffectiveFrom(), existing.getEffectiveTo())) {
                throw new BizException(400, MessageKeys.BILLING_BINDING_OVERLAP);
            }
        }
    }

    private static boolean sameColor(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        return a != null && a.equals(b);
    }

    /** 生效区间 [from,to]，两端可空代表不限；判断两个区间是否相交（边界相邻视为不重叠）。 */
    private static boolean rangesOverlap(LocalDate fromA, LocalDate toA,
                                         LocalDate fromB, LocalDate toB) {
        if (endsBefore(fromA, toA, fromB, toB) || endsBefore(fromB, toB, fromA, toA)) {
            return false;
        }
        return true;
    }

    /** 区间 A 是否整体结束在区间 B 开始之前（任一缺失边界视为无限，不满足该条件）。 */
    private static boolean endsBefore(LocalDate fromA, LocalDate toA,
                                      LocalDate fromB, LocalDate toB) {
        return toA != null && fromB != null && toA.isBefore(fromB);
    }

    private record LotBindingInput(Long lotId, String ruleType, Long ruleId, String plateColor,
                                   LocalDate effectiveFrom, LocalDate effectiveTo) {
    }

    private LotBindingInput normalize(BillingLotBindingRequest request) {
        if (request == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        Long lotId = request.lotId();
        if (lotId == null || lotId <= 0) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        String ruleType = normalizeRuleType(request.ruleType());
        Long ruleId = requireTemplate(ruleType, request.ruleId());
        String plateColor = normalizePlateColor(request.plateColor());
        LocalDate from = request.effectiveFrom();
        LocalDate to = request.effectiveTo();
        if (from != null && to != null && to.isBefore(from)) {
            throw new BizException(400, MessageKeys.BILLING_BINDING_INVALID_RANGE);
        }
        return new LotBindingInput(lotId, ruleType, ruleId, plateColor, from, to);
    }

    private String normalizeRuleType(String raw) {
        String value = raw == null ? null : raw.trim().toUpperCase(Locale.ROOT);
        if (BillingLotBinding.TYPE_DAILY.equals(value) || BillingLotBinding.TYPE_GENERAL.equals(value)) {
            return value;
        }
        throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
    }

    /** 所选模板必须真实存在，且类型与 ruleType 匹配。 */
    private Long requireTemplate(String ruleType, Long ruleId) {
        if (ruleId == null || ruleId <= 0) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        boolean exists = BillingLotBinding.TYPE_DAILY.equals(ruleType)
                ? dailyRules.existsById(ruleId)
                : generalRules.existsById(ruleId);
        if (!exists) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return ruleId;
    }

    private String normalizePlateColor(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String color = value.trim().toUpperCase(Locale.ROOT);
        if (color.length() > MAX_COLOR_LENGTH || !color.matches("[A-Z0-9_]+")) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return color;
    }

    private BillingLotBinding requireBinding(Long bindingId) {
        return bindings.findById(bindingId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
    }

    /** 解析模板当前标题（模板被删导致悬空时返回 null）。 */
    private String resolveRuleTitle(String ruleType, Long ruleId) {
        return BillingLotBinding.TYPE_DAILY.equals(ruleType)
                ? dailyRules.findById(ruleId).map(dailyRule -> dailyRule.getTitle()).orElse(null)
                : generalRules.findById(ruleId).map(generalRule -> generalRule.getTitle()).orElse(null);
    }

    private BillingLotBindingView toView(BillingLotBinding binding) {
        return new BillingLotBindingView(
                binding.getId(),
                binding.getLotId(),
                binding.getRuleType(),
                binding.getRuleId(),
                resolveRuleTitle(binding.getRuleType(), binding.getRuleId()),
                binding.getPlateColor(),
                binding.getEffectiveFrom(),
                binding.getEffectiveTo(),
                binding.getCreatedAt(),
                binding.getUpdatedAt());
    }
}

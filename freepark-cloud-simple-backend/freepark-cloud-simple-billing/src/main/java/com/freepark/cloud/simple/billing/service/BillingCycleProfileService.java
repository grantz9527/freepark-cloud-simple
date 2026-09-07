package com.freepark.cloud.simple.billing.service;

import com.freepark.cloud.simple.billing.dto.BillingCycleProfileRequest;
import com.freepark.cloud.simple.billing.dto.BillingCycleProfileView;
import com.freepark.cloud.simple.billing.dto.BillingCycleSegmentRequest;
import com.freepark.cloud.simple.billing.dto.BillingCycleSegmentView;
import com.freepark.cloud.simple.billing.entity.BillingCycleProfile;
import com.freepark.cloud.simple.billing.entity.BillingCycleSegment;
import com.freepark.cloud.simple.billing.entity.CycleTailMode;
import com.freepark.cloud.simple.billing.repository.BillingCycleProfileRepository;
import com.freepark.cloud.simple.billing.repository.BillingCycleSegmentRepository;
import com.freepark.cloud.simple.billing.repository.BillingGeneralRuleRepository;
import com.freepark.cloud.simple.billing.repository.BillingDailyRuleRepository;
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
import java.util.List;

/**
 * 计费周期方案服务（全局共享配置，供各车场 24 小时制规则选用）。
 * <p>
 * 方案由有序分段行组成，每行可带重复次数（默认 1）。计费消费方把每行按重复次数展开成
 * 连续档位后逐档顺序消耗；全部档位用尽后超出的时长不再收费，尾数时长按方案的尾数方式计费。
 * 方案作为 24 小时制计费规则的分段模板，满 24 小时由计费方按方案自动重新开始下一轮，因此
 * 不对方案的「展开后总时长」设上限，重复次数上限以 {@link #MAX_REPEAT} 为准。所有写操作需登录管理员。
 * </p>
 */
@Service
public class BillingCycleProfileService {

    /** 单行每档时长上限（单档不超过 24 小时） */
    private static final int MAX_SEGMENT_MINUTES = 1440;

    /** 方案填写分段行数上限 */
    private static final int MAX_SEGMENTS = 48;

    /** 单行重复次数上限 */
    private static final int MAX_REPEAT = 99999;

    /** 单行金额上限 */
    private static final BigDecimal MAX_MONEY = new BigDecimal("99999999.99");

    private static final int MAX_NAME_LENGTH = 80;
    private static final int MAX_DESCRIPTION_LENGTH = 255;

    private final BillingCycleProfileRepository profiles;
    private final BillingCycleSegmentRepository segments;
    private final BillingGeneralRuleRepository rules;
    private final BillingDailyRuleRepository dailyRules;
    private final AdminGuard adminGuard;

    public BillingCycleProfileService(BillingCycleProfileRepository profiles,
                                      BillingCycleSegmentRepository segments,
                                      BillingGeneralRuleRepository rules,
                                      BillingDailyRuleRepository dailyRules,
                                      AdminGuard adminGuard) {
        this.profiles = profiles;
        this.segments = segments;
        this.rules = rules;
        this.dailyRules = dailyRules;
        this.adminGuard = adminGuard;
    }

    @Transactional(readOnly = true)
    public List<BillingCycleProfileView> list() {
        List<BillingCycleProfile> entries = profiles.findAllByOrderByIdAsc();
        return entries.stream()
                .map(this::toView)
                .toList();
    }

    @Transactional
    public BillingCycleProfileView create(BillingCycleProfileRequest request) {
        adminGuard.requireEnabledAdmin();
        ProfileInput input = normalize(request);
        BillingCycleProfile profile = new BillingCycleProfile();
        profile.setName(input.name());
        profile.setDescription(input.description());
        profile.setTailMode(input.tailMode().name());
        LocalDateTime now = SiteZoneTimes.nowUtc();
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        profile = profiles.save(profile);
        saveSegments(profile.getId(), input.segments());
        return toView(profile);
    }

    @Transactional
    public BillingCycleProfileView update(Long profileId, BillingCycleProfileRequest request) {
        adminGuard.requireEnabledAdmin();
        BillingCycleProfile profile = requireProfile(profileId);
        ProfileInput input = normalize(request);
        profile.setName(input.name());
        profile.setDescription(input.description());
        profile.setTailMode(input.tailMode().name());
        profile.setUpdatedAt(SiteZoneTimes.nowUtc());
        profiles.save(profile);
        // 分段整组替换，避免残留过期行
        segments.deleteByProfileId(profile.getId());
        saveSegments(profile.getId(), input.segments());
        return toView(profile);
    }

    @Transactional
    public void delete(Long profileId) {
        adminGuard.requireEnabledAdmin();
        BillingCycleProfile profile = requireProfile(profileId);
        if (!rules.findByCycleProfileId(profileId).isEmpty()
                || !dailyRules.findByCycleProfileId(profileId).isEmpty()) {
            throw new BizException(400, MessageKeys.BILLING_PROFILE_REFERENCED);
        }
        segments.deleteByProfileId(profileId);
        profiles.delete(profile);
    }

    private void saveSegments(Long profileId, List<BillingCycleSegmentRequest> items) {
        int seq = 1;
        for (BillingCycleSegmentRequest item : items) {
            BillingCycleSegment segment = new BillingCycleSegment();
            segment.setProfileId(profileId);
            segment.setSeq(seq++);
            segment.setMinutes(item.minutes());
            segment.setUnitPriceYuan(item.unitPriceYuan());
            segment.setRepeatCount(item.repeatCount());
            segments.save(segment);
        }
    }

    private record ProfileInput(String name, String description, CycleTailMode tailMode,
                                List<BillingCycleSegmentRequest> segments) {
    }

    private ProfileInput normalize(BillingCycleProfileRequest request) {
        if (request == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        String name = normalizeName(request.name());
        String description = normalizeDescription(request.description());
        CycleTailMode tailMode = parseTailMode(request.tailMode());
        List<BillingCycleSegmentRequest> items = normalizeSegments(request.segments());
        return new ProfileInput(name, description, tailMode, items);
    }

    private String normalizeName(String value) {
        String name = value == null ? null : value.trim();
        if (!StringUtils.hasText(name)) {
            throw new BizException(400, MessageKeys.BILLING_PROFILE_NAME_REQUIRED);
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return name;
    }

    private String normalizeDescription(String value) {
        String description = value == null ? null : value.trim();
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return StringUtils.hasText(description) ? description : null;
    }

    private CycleTailMode parseTailMode(String raw) {
        if (StringUtils.hasText(raw)) {
            for (CycleTailMode candidate : CycleTailMode.values()) {
                if (candidate.name().equalsIgnoreCase(raw.trim())) {
                    return candidate;
                }
            }
        }
        throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
    }

    private List<BillingCycleSegmentRequest> normalizeSegments(List<BillingCycleSegmentRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new BizException(400, MessageKeys.BILLING_PROFILE_SEGMENTS_REQUIRED);
        }
        if (items.size() > MAX_SEGMENTS) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        List<BillingCycleSegmentRequest> normalized = new ArrayList<>(items.size());
        for (BillingCycleSegmentRequest item : items) {
            if (item == null) {
                throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
            }
            int minutes = item.minutes();
            if (minutes < 1 || minutes > MAX_SEGMENT_MINUTES) {
                throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
            }
            BigDecimal price = normalizeMoney(item.unitPriceYuan());
            // 重复次数缺省按 1；展开后总时长不设上限（满 24 小时由计费方按方案自动重开）
            int repeat = item.repeatCount() == null ? 1 : item.repeatCount();
            if (repeat < 1 || repeat > MAX_REPEAT) {
                throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
            }
            normalized.add(new BillingCycleSegmentRequest(minutes, price, repeat));
        }
        return normalized;
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

    private BillingCycleProfile requireProfile(Long profileId) {
        return profiles.findById(profileId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
    }

    private BillingCycleProfileView toView(BillingCycleProfile profile) {
        List<BillingCycleSegmentView> segmentViews = segments.findByProfileIdOrderBySeqAsc(profile.getId())
                .stream()
                .map(segment -> new BillingCycleSegmentView(
                        segment.getId(),
                        segment.getSeq(),
                        segment.getMinutes(),
                        segment.getUnitPriceYuan(),
                        segment.getRepeatCount()))
                .toList();
        return new BillingCycleProfileView(
                profile.getId(),
                profile.getName(),
                profile.getDescription(),
                profile.getTailMode(),
                segmentViews,
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }
}

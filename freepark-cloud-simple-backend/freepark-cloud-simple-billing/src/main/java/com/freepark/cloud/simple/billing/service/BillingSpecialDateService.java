package com.freepark.cloud.simple.billing.service;

import com.freepark.cloud.simple.billing.dto.BillingDateRequest;
import com.freepark.cloud.simple.billing.dto.BillingSpecialDateView;
import com.freepark.cloud.simple.billing.entity.BillingSpecialDate;
import com.freepark.cloud.simple.billing.entity.SpecialDateType;
import com.freepark.cloud.simple.billing.repository.BillingSpecialDateRepository;
import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.user.service.AdminGuard;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 计费特殊时间段（节假日 / 补班，精确到分钟）服务。
 * <p>
 * 时间语义（与全系统一致，由全局 Jackson 时区换算统一处理，本服务不再手工换算）：
 * 库内统一按「UTC 挂钟时间」存放作为绝对时刻锚点；请求中的时间是「系统配置时区」的
 * 本地挂钟时间，反序列化时自动换算成 UTC 锚点入库；响应时 View 中的 UTC 锚点由序列化器
 * 自动换算回「系统配置时区」的本地时间。因此切换系统配置时区后，同一记录的展示时间会
 * 随时区整体偏移（如 UTC ↔ 上海相差 8 小时）。
 */
@Service
public class BillingSpecialDateService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_NAME_LENGTH = 80;

    private final BillingSpecialDateRepository dates;
    private final AdminGuard adminGuard;

    public BillingSpecialDateService(BillingSpecialDateRepository dates,
                                     AdminGuard adminGuard) {
        this.dates = dates;
        this.adminGuard = adminGuard;
    }

    @Transactional(readOnly = true)
    public PageResult<BillingSpecialDateView> list(String rawType, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        SpecialDateType type = parseOptionalType(rawType);
        Specification<BillingSpecialDate> spec = buildSpec(type);
        Page<BillingSpecialDate> result = dates.findAll(spec,
                PageRequest.of(safePage - 1, safeSize,
                        Sort.by(Sort.Direction.ASC, "startTime", "endTime", "id")));
        List<BillingSpecialDateView> items = result.getContent().stream()
                .map(this::toView)
                .toList();
        return PageResult.of(items, result.getTotalElements(), safePage, safeSize);
    }

    @Transactional
    public BillingSpecialDateView create(BillingDateRequest request) {
        adminGuard.requireEnabledAdmin();
        SpecialDateType type = parseType(request == null ? null : request.type());
        String name = normalizeName(request == null ? null : request.name());
        // 请求时间已由全局反序列化器换算成 UTC 锚点
        LocalDateTime start = requireDateTime(request == null ? null : request.startTime());
        LocalDateTime end = requireDateTime(request == null ? null : request.endTime());
        validateRange(start, end);
        if (dates.overlapsExcluding(start, end, null)) {
            throw new BizException(400, MessageKeys.BILLING_DATE_OVERLAP);
        }
        BillingSpecialDate entry = new BillingSpecialDate();
        entry.setType(type);
        entry.setName(name);
        entry.setStartTime(start);
        entry.setEndTime(end);
        LocalDateTime now = SiteZoneTimes.nowUtc();
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);
        return toView(dates.save(entry));
    }

    @Transactional
    public BillingSpecialDateView update(Long dateId, BillingDateRequest request) {
        adminGuard.requireEnabledAdmin();
        BillingSpecialDate entry = requireEntry(dateId);
        SpecialDateType type = parseType(request == null ? null : request.type());
        String name = normalizeName(request == null ? null : request.name());
        LocalDateTime start = requireDateTime(request == null ? null : request.startTime());
        LocalDateTime end = requireDateTime(request == null ? null : request.endTime());
        validateRange(start, end);
        if (dates.overlapsExcluding(start, end, dateId)) {
            throw new BizException(400, MessageKeys.BILLING_DATE_OVERLAP);
        }
        entry.setType(type);
        entry.setName(name);
        entry.setStartTime(start);
        entry.setEndTime(end);
        entry.setUpdatedAt(SiteZoneTimes.nowUtc());
        return toView(dates.save(entry));
    }

    @Transactional
    public void delete(Long dateId) {
        adminGuard.requireEnabledAdmin();
        dates.delete(requireEntry(dateId));
    }

    private Specification<BillingSpecialDate> buildSpec(SpecialDateType type) {
        return (root, query, cb) -> {
            if (type == null) {
                return cb.conjunction();
            }
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("type"), type));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private SpecialDateType parseOptionalType(String raw) {
        return StringUtils.hasText(raw) ? parseType(raw) : null;
    }

    private SpecialDateType parseType(String raw) {
        if (StringUtils.hasText(raw)) {
            String value = raw.trim();
            for (SpecialDateType candidate : SpecialDateType.values()) {
                if (candidate.name().equalsIgnoreCase(value)) {
                    return candidate;
                }
            }
        }
        throw new BizException(400, MessageKeys.BILLING_DATE_INVALID_TYPE);
    }

    private String normalizeName(String value) {
        String trimmed = value == null ? null : value.trim();
        if (trimmed != null && trimmed.length() > MAX_NAME_LENGTH) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }

    private LocalDateTime requireDateTime(LocalDateTime time) {
        if (time == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return time;
    }

    /**
     * 组装视图：直接透传库内 UTC 锚点，响应序列化时统一换算为「当前系统配置时区」本地时间。
     */
    private BillingSpecialDateView toView(BillingSpecialDate entry) {
        return new BillingSpecialDateView(
                entry.getId(),
                entry.getType().name(),
                entry.getName(),
                entry.getStartTime(),
                entry.getEndTime(),
                entry.getCreatedAt(),
                entry.getUpdatedAt());
    }

    private void validateRange(LocalDateTime start, LocalDateTime end) {
        if (!start.isBefore(end)) {
            throw new BizException(400, MessageKeys.BILLING_DATE_INVALID_RANGE);
        }
    }

    private BillingSpecialDate requireEntry(Long dateId) {
        return dates.findById(dateId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
    }
}

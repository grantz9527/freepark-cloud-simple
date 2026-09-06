package com.freepark.cloud.simple.parking.service;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.parking.dto.CreatePatternAllowlistRequest;
import com.freepark.cloud.simple.parking.dto.PatternAllowlistView;
import com.freepark.cloud.simple.parking.dto.UpdatePatternAllowlistRequest;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.entity.PatternAllowlist;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.parking.repository.PatternAllowlistRepository;
import com.freepark.cloud.simple.user.service.AdminGuard;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 放行名单（车牌号段规则）服务。
 */
@Service
public class PatternAllowlistService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ParkingLotRepository lots;
    private final PatternAllowlistRepository entries;
    private final AdminGuard adminGuard;

    public PatternAllowlistService(ParkingLotRepository lots,
                                   PatternAllowlistRepository entries,
                                   AdminGuard adminGuard) {
        this.lots = lots;
        this.entries = entries;
        this.adminGuard = adminGuard;
    }

    @Transactional(readOnly = true)
    public PageResult<PatternAllowlistView> listEntries(Long lotId, String keyword, int page, int size) {
        requireLot(lotId);
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String trimmed = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Specification<PatternAllowlist> spec = buildSpec(lotId, trimmed);
        Page<PatternAllowlist> result = entries.findAll(spec,
                PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.ASC, "name")));
        List<PatternAllowlistView> items = result.getContent().stream().map(PatternAllowlistView::from).toList();
        return PageResult.of(items, result.getTotalElements(), safePage, safeSize);
    }

    @Transactional
    public PatternAllowlistView createEntry(Long lotId, CreatePatternAllowlistRequest request) {
        adminGuard.requireEnabledAdmin();
        ParkingLot lot = requireLot(lotId);
        String name = requireText(request == null ? null : request.name(), 80);
        String pattern = validatePattern(request == null ? null : request.pattern());
        if (entries.existsByLotIdAndNameIgnoreCase(lotId, name)) {
            throw new BizException(400, MessageKeys.PARKING_PATTERN_ALLOWLIST_NAME_EXISTS);
        }
        if (entries.existsByLotIdAndPattern(lotId, pattern)) {
            throw new BizException(400, MessageKeys.PARKING_PATTERN_ALLOWLIST_PATTERN_EXISTS);
        }
        boolean enabled = request.enabled() == null || request.enabled();
        PatternAllowlist entry = new PatternAllowlist();
        entry.setLot(lot);
        entry.setName(name);
        entry.setPattern(pattern);
        entry.setRemark(normalizeOptional(request.remark()));
        entry.setEnabled(enabled);
        return PatternAllowlistView.from(entries.save(entry));
    }

    @Transactional
    public PatternAllowlistView updateEntry(Long lotId, Long entryId, UpdatePatternAllowlistRequest request) {
        adminGuard.requireEnabledAdmin();
        requireLot(lotId);
        PatternAllowlist entry = requireEntry(entryId);
        if (!entry.getLot().getId().equals(lotId)) {
            throw new BizException(404, MessageKeys.COMMON_NOT_FOUND);
        }
        String name = requireText(request == null ? null : request.name(), 80);
        String pattern = validatePattern(request == null ? null : request.pattern());
        if (entries.existsByLotIdAndNameIgnoreCaseAndIdNot(lotId, name, entryId)) {
            throw new BizException(400, MessageKeys.PARKING_PATTERN_ALLOWLIST_NAME_EXISTS);
        }
        if (entries.existsByLotIdAndPatternAndIdNot(lotId, pattern, entryId)) {
            throw new BizException(400, MessageKeys.PARKING_PATTERN_ALLOWLIST_PATTERN_EXISTS);
        }
        boolean enabled = request.enabled() == null ? entry.isEnabled() : request.enabled();
        entry.setName(name);
        entry.setPattern(pattern);
        entry.setRemark(normalizeOptional(request.remark()));
        entry.setEnabled(enabled);
        return PatternAllowlistView.from(entries.save(entry));
    }

    @Transactional
    public void deleteEntry(Long lotId, Long entryId) {
        adminGuard.requireEnabledAdmin();
        requireLot(lotId);
        PatternAllowlist entry = requireEntry(entryId);
        if (!entry.getLot().getId().equals(lotId)) {
            throw new BizException(404, MessageKeys.COMMON_NOT_FOUND);
        }
        entries.delete(entry);
    }

    private Specification<PatternAllowlist> buildSpec(Long lotId, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("lot").get("id"), lotId));
            if (keyword != null && !keyword.isEmpty()) {
                String like = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("pattern")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String validatePattern(String raw) {
        String pattern = requireText(raw, 255);
        try {
            Pattern.compile(pattern);
        } catch (PatternSyntaxException ex) {
            throw new BizException(400, MessageKeys.PARKING_PATTERN_ALLOWLIST_INVALID_PATTERN);
        }
        return pattern;
    }

    private ParkingLot requireLot(Long lotId) {
        return lots.findById(lotId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
    }

    private PatternAllowlist requireEntry(Long entryId) {
        return entries.findById(entryId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
    }

    private String requireText(String value, int maxLength) {
        String trimmed = value == null ? null : value.trim();
        if (!StringUtils.hasText(trimmed) || trimmed.length() > maxLength) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return trimmed;
    }

    private String normalizeOptional(String value) {
        String trimmed = value == null ? null : value.trim();
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }
}

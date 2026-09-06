package com.freepark.cloud.simple.parking.service;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.parking.dto.BoothView;
import com.freepark.cloud.simple.parking.dto.CreateBoothRequest;
import com.freepark.cloud.simple.parking.dto.UpdateBoothRequest;
import com.freepark.cloud.simple.parking.entity.ParkingBooth;
import com.freepark.cloud.simple.parking.entity.ParkingLane;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.repository.ParkingBoothRepository;
import com.freepark.cloud.simple.parking.repository.ParkingLaneRepository;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
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

/**
 * 岗亭服务。
 */
@Service
public class ParkingBoothService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ParkingLotRepository lots;
    private final ParkingBoothRepository booths;
    private final ParkingLaneRepository lanes;
    private final AdminGuard adminGuard;

    public ParkingBoothService(ParkingLotRepository lots,
                               ParkingBoothRepository booths,
                               ParkingLaneRepository lanes,
                               AdminGuard adminGuard) {
        this.lots = lots;
        this.booths = booths;
        this.lanes = lanes;
        this.adminGuard = adminGuard;
    }

    @Transactional(readOnly = true)
    public PageResult<BoothView> listBooths(Long lotId, String keyword, int page, int size) {
        requireLot(lotId);
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Specification<ParkingBooth> spec = buildSpec(lotId, trimmedKeyword);
        Page<ParkingBooth> result = booths.findAll(spec,
                PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.ASC, "name")));
        List<BoothView> items = result.getContent().stream().map(BoothView::from).toList();
        return PageResult.of(items, result.getTotalElements(), safePage, safeSize);
    }

    @Transactional
    public BoothView createBooth(Long lotId, CreateBoothRequest request) {
        adminGuard.requireEnabledAdmin();
        ParkingLot lot = requireLot(lotId);
        String name = normalizeRequired(request == null ? null : request.name());
        if (booths.existsByLotIdAndNameIgnoreCase(lotId, name)) {
            throw new BizException(400, MessageKeys.PARKING_BOOTH_NAME_EXISTS);
        }
        String code = normalizeOptional(request.code());
        if (code != null && booths.existsByLotIdAndCodeIgnoreCase(lotId, code)) {
            throw new BizException(400, MessageKeys.PARKING_BOOTH_CODE_EXISTS);
        }
        boolean enabled = request.enabled() == null || request.enabled();
        ParkingBooth booth = new ParkingBooth();
        booth.setLot(lot);
        booth.setName(name);
        booth.setCode(code);
        booth.setLocation(normalizeOptional(request.location()));
        booth.setEnabled(enabled);
        booth.setLanes(resolveLanes(lotId, request.laneIds()));
        return BoothView.from(booths.save(booth));
    }

    @Transactional
    public BoothView updateBooth(Long lotId, Long boothId, UpdateBoothRequest request) {
        adminGuard.requireEnabledAdmin();
        requireLot(lotId);
        ParkingBooth booth = requireBooth(boothId);
        if (!booth.getLot().getId().equals(lotId)) {
            throw new BizException(404, MessageKeys.COMMON_NOT_FOUND);
        }
        String name = normalizeRequired(request == null ? null : request.name());
        if (booths.existsByLotIdAndNameIgnoreCaseAndIdNot(lotId, name, boothId)) {
            throw new BizException(400, MessageKeys.PARKING_BOOTH_NAME_EXISTS);
        }
        String code = normalizeOptional(request.code());
        if (code != null && booths.existsByLotIdAndCodeIgnoreCaseAndIdNot(lotId, code, boothId)) {
            throw new BizException(400, MessageKeys.PARKING_BOOTH_CODE_EXISTS);
        }
        boolean enabled = request.enabled() == null ? booth.isEnabled() : request.enabled();
        booth.setName(name);
        booth.setCode(code);
        booth.setLocation(normalizeOptional(request.location()));
        booth.setEnabled(enabled);
        if (request.laneIds() != null) {
            booth.setLanes(resolveLanes(lotId, request.laneIds()));
        }
        return BoothView.from(booths.save(booth));
    }

    @Transactional
    public void deleteBooth(Long lotId, Long boothId) {
        adminGuard.requireEnabledAdmin();
        requireLot(lotId);
        ParkingBooth booth = requireBooth(boothId);
        if (!booth.getLot().getId().equals(lotId)) {
            throw new BizException(404, MessageKeys.COMMON_NOT_FOUND);
        }
        booths.delete(booth);
    }

    private Specification<ParkingBooth> buildSpec(Long lotId, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("lot").get("id"), lotId));
            if (keyword != null && !keyword.isEmpty()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("code")), pattern)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private List<ParkingLane> resolveLanes(Long lotId, List<Long> laneIds) {
        if (laneIds == null || laneIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<ParkingLane> resolved = new ArrayList<>();
        for (Long laneId : laneIds) {
            ParkingLane lane = lanes.findById(laneId)
                    .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
            if (!lane.getLot().getId().equals(lotId)) {
                throw new BizException(404, MessageKeys.COMMON_NOT_FOUND);
            }
            resolved.add(lane);
        }
        return resolved;
    }

    private ParkingLot requireLot(Long lotId) {
        return lots.findById(lotId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
    }

    private ParkingBooth requireBooth(Long boothId) {
        return booths.findById(boothId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
    }

    private String normalizeRequired(String value) {
        String trimmed = value == null ? null : value.trim();
        if (!StringUtils.hasText(trimmed)) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return trimmed;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

package com.freepark.cloud.simple.parking.service;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.parking.dto.AreaView;
import com.freepark.cloud.simple.parking.dto.CreateAreaRequest;
import com.freepark.cloud.simple.parking.dto.CreateLocationRequest;
import com.freepark.cloud.simple.parking.dto.CreateSpaceRequest;
import com.freepark.cloud.simple.parking.dto.LocationView;
import com.freepark.cloud.simple.parking.dto.SpaceView;
import com.freepark.cloud.simple.parking.dto.UpdateSpaceRequest;
import com.freepark.cloud.simple.parking.entity.ParkingArea;
import com.freepark.cloud.simple.parking.entity.ParkingLocation;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.entity.ParkingSpace;
import com.freepark.cloud.simple.parking.repository.ParkingAreaRepository;
import com.freepark.cloud.simple.parking.repository.ParkingLocationRepository;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.parking.repository.ParkingSpaceRepository;
import com.freepark.cloud.simple.parking.support.ParkingSpreadsheetSupport;
import com.freepark.cloud.simple.user.service.AdminGuard;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 车位分层服务：位置(location) -> 区域(area) -> 车位(space)，含 Excel 导入。
 */
@Service
public class ParkingSpaceService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ParkingLotRepository lots;
    private final ParkingLocationRepository locations;
    private final ParkingAreaRepository areas;
    private final ParkingSpaceRepository spaces;
    private final AdminGuard adminGuard;

    public ParkingSpaceService(ParkingLotRepository lots,
                               ParkingLocationRepository locations,
                               ParkingAreaRepository areas,
                               ParkingSpaceRepository spaces,
                               AdminGuard adminGuard) {
        this.lots = lots;
        this.locations = locations;
        this.areas = areas;
        this.spaces = spaces;
        this.adminGuard = adminGuard;
    }

    @Transactional(readOnly = true)
    public List<LocationView> listLocations(Long lotId) {
        requireLot(lotId);
        return locations.findByLotIdOrderByNameAsc(lotId).stream()
                .map(LocationView::from)
                .toList();
    }

    @Transactional
    public LocationView createLocation(Long lotId, CreateLocationRequest request) {
        adminGuard.requireEnabledAdmin();
        ParkingLot lot = requireLot(lotId);
        String name = normalizeRequired(request == null ? null : request.name());
        if (locations.existsByLotIdAndNameIgnoreCase(lotId, name)) {
            throw new BizException(400, MessageKeys.PARKING_LOCATION_NAME_EXISTS);
        }
        ParkingLocation location = new ParkingLocation();
        location.setLot(lot);
        location.setName(name);
        return LocationView.from(locations.save(location));
    }

    @Transactional(readOnly = true)
    public List<AreaView> listAreas(Long lotId, Long locationId) {
        requireLot(lotId);
        if (locationId != null) {
            ParkingLocation location = requireLocation(locationId);
            if (!location.getLot().getId().equals(lotId)) {
                throw new BizException(404, MessageKeys.COMMON_NOT_FOUND);
            }
            return areas.findByLocationIdOrderByNameAsc(locationId).stream()
                    .map(AreaView::from)
                    .toList();
        }
        return areas.findByLocationLotIdOrderByNameAsc(lotId).stream()
                .map(AreaView::from)
                .toList();
    }

    @Transactional
    public AreaView createArea(Long lotId, CreateAreaRequest request) {
        adminGuard.requireEnabledAdmin();
        requireLot(lotId);
        Long locationId = request == null ? null : request.locationId();
        if (locationId == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        ParkingLocation location = requireLocation(locationId);
        if (!location.getLot().getId().equals(lotId)) {
            throw new BizException(404, MessageKeys.COMMON_NOT_FOUND);
        }
        String name = normalizeRequired(request.name());
        if (areas.existsByLocationIdAndNameIgnoreCase(locationId, name)) {
            throw new BizException(400, MessageKeys.PARKING_AREA_NAME_EXISTS);
        }
        ParkingArea area = new ParkingArea();
        area.setLocation(location);
        area.setName(name);
        return AreaView.from(areas.save(area));
    }

    @Transactional(readOnly = true)
    public PageResult<SpaceView> listSpaces(Long lotId, Long locationId, Long areaId,
                                            String code, int page, int size) {
        requireLot(lotId);
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String trimmedCode = StringUtils.hasText(code) ? code.trim() : null;
        Specification<ParkingSpace> spec = buildSpaceSpec(lotId, locationId, areaId, trimmedCode);
        Page<ParkingSpace> result = spaces.findAll(spec,
                PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.ASC, "code")));
        List<SpaceView> items = result.getContent().stream().map(SpaceView::from).toList();
        return PageResult.of(items, result.getTotalElements(), safePage, safeSize);
    }

    @Transactional
    public SpaceView createSpace(Long lotId, CreateSpaceRequest request) {
        adminGuard.requireEnabledAdmin();
        ParkingLot lot = requireLot(lotId);
        Long areaId = request == null ? null : request.areaId();
        if (areaId == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        ParkingArea area = requireAreaForLot(lotId, areaId);
        String code = normalizeRequired(request.code());
        if (spaces.existsByLotIdAndCodeIgnoreCase(lotId, code)) {
            throw new BizException(400, MessageKeys.PARKING_SPACE_CODE_EXISTS);
        }
        boolean enabled = request.enabled() == null || request.enabled();
        ParkingSpace space = new ParkingSpace();
        space.setLot(lot);
        space.setArea(area);
        space.setCode(code);
        space.setEnabled(enabled);
        return SpaceView.from(spaces.save(space));
    }

    @Transactional
    public SpaceView updateSpace(Long lotId, Long spaceId, UpdateSpaceRequest request) {
        adminGuard.requireEnabledAdmin();
        requireLot(lotId);
        ParkingSpace space = requireSpace(spaceId);
        if (!space.getLot().getId().equals(lotId)) {
            throw new BizException(404, MessageKeys.COMMON_NOT_FOUND);
        }
        Long areaId = request == null ? null : request.areaId();
        if (areaId == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        ParkingArea area = requireAreaForLot(lotId, areaId);
        String code = normalizeRequired(request.code());
        if (spaces.existsByLotIdAndCodeIgnoreCaseAndIdNot(lotId, code, spaceId)) {
            throw new BizException(400, MessageKeys.PARKING_SPACE_CODE_EXISTS);
        }
        boolean enabled = request.enabled() == null ? space.isEnabled() : request.enabled();
        space.setArea(area);
        space.setCode(code);
        space.setEnabled(enabled);
        return SpaceView.from(spaces.save(space));
    }

    @Transactional
    public void deleteSpace(Long lotId, Long spaceId) {
        adminGuard.requireEnabledAdmin();
        requireLot(lotId);
        ParkingSpace space = requireSpace(spaceId);
        if (!space.getLot().getId().equals(lotId)) {
            throw new BizException(404, MessageKeys.COMMON_NOT_FOUND);
        }
        spaces.delete(space);
    }

    @Transactional(readOnly = true)
    public byte[] buildImportTemplate(Long lotId) {
        requireLot(lotId);
        return ParkingSpreadsheetSupport.buildTemplate("泊位", ParkingSpreadsheetSupport.SPACE_TEMPLATE_COLUMNS);
    }

    @Transactional
    public int importSpaces(Long lotId, Long areaId, MultipartFile file) {
        adminGuard.requireEnabledAdmin();
        ParkingLot lot = requireLot(lotId);
        if (areaId == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        ParkingArea area = requireAreaForLot(lotId, areaId);
        List<String[]> rows = ParkingSpreadsheetSupport.readRows(file,
                ParkingSpreadsheetSupport.SPACE_COLUMN_COUNT);
        int imported = 0;
        for (String[] cells : rows) {
            String code = ParkingSpreadsheetSupport.cell(cells, 0);
            if (code.isEmpty()) {
                continue;
            }
            if (spaces.existsByLotIdAndCodeIgnoreCase(lotId, code)) {
                continue;
            }
            ParkingSpace space = new ParkingSpace();
            space.setLot(lot);
            space.setArea(area);
            space.setCode(code);
            space.setEnabled(true);
            spaces.save(space);
            imported++;
        }
        return imported;
    }

    private Specification<ParkingSpace> buildSpaceSpec(Long lotId, Long locationId, Long areaId, String code) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("lot").get("id"), lotId));
            Join<ParkingSpace, ParkingArea> areaJoin = root.join("area");
            if (areaId != null) {
                predicates.add(cb.equal(areaJoin.get("id"), areaId));
            } else if (locationId != null) {
                predicates.add(cb.equal(areaJoin.get("location").get("id"), locationId));
            }
            if (code != null && !code.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("code")), "%" + code.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private ParkingLot requireLot(Long lotId) {
        return lots.findById(lotId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
    }

    private ParkingLocation requireLocation(Long locationId) {
        return locations.findById(locationId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
    }

    private ParkingArea requireAreaForLot(Long lotId, Long areaId) {
        ParkingArea area = areas.findById(areaId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
        if (!area.getLocation().getLot().getId().equals(lotId)) {
            throw new BizException(404, MessageKeys.COMMON_NOT_FOUND);
        }
        return area;
    }

    private ParkingSpace requireSpace(Long spaceId) {
        return spaces.findById(spaceId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
    }

    private String normalizeRequired(String value) {
        String trimmed = value == null ? null : value.trim();
        if (!StringUtils.hasText(trimmed)) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return trimmed;
    }
}

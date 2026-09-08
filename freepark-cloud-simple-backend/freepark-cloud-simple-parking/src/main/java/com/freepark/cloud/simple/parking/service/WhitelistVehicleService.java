package com.freepark.cloud.simple.parking.service;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.time.SiteZoneProvider;
import com.freepark.cloud.simple.common.time.SiteZoneTimes;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.parking.dto.CreateWhitelistVehicleRequest;
import com.freepark.cloud.simple.parking.dto.UpdateWhitelistVehicleRequest;
import com.freepark.cloud.simple.parking.dto.VehicleImportResult;
import com.freepark.cloud.simple.parking.dto.WhitelistVehicleView;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.entity.PlateColor;
import com.freepark.cloud.simple.parking.entity.VehicleType;
import com.freepark.cloud.simple.parking.entity.WhitelistVehicle;
import com.freepark.cloud.simple.parking.edge.EdgeDomainChangeNotifier;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.parking.repository.WhitelistVehicleRepository;
import com.freepark.cloud.simple.parking.support.ParkingSpreadsheetSupport;
import com.freepark.cloud.simple.settings.runtime.EdgeConfigSyncProtocol;
import com.freepark.cloud.simple.user.service.AdminGuard;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 白名单车辆（停车卡）服务：同一车牌允许多张停车卡（不同时间区间）。
 */
@Service
public class WhitelistVehicleService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_PLATE_LENGTH = 20;
    private static final int MAX_OWNER_LENGTH = 80;
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ParkingLotRepository lots;
    private final WhitelistVehicleRepository vehicles;
    private final AdminGuard adminGuard;
    private final SiteZoneProvider siteZoneProvider;
    private final EdgeDomainChangeNotifier notifier;

    public WhitelistVehicleService(ParkingLotRepository lots,
                                   WhitelistVehicleRepository vehicles,
                                   AdminGuard adminGuard,
                                   SiteZoneProvider siteZoneProvider,
                                   EdgeDomainChangeNotifier notifier) {
        this.lots = lots;
        this.vehicles = vehicles;
        this.adminGuard = adminGuard;
        this.siteZoneProvider = siteZoneProvider;
        this.notifier = notifier;
    }

    @Transactional(readOnly = true)
    public PageResult<WhitelistVehicleView> listVehicles(Long lotId, String plate, int page, int size) {
        requireLot(lotId);
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String trimmedPlate = StringUtils.hasText(plate) ? plate.trim() : null;
        Specification<WhitelistVehicle> spec = buildSpec(lotId, trimmedPlate);
        Page<WhitelistVehicle> result = vehicles.findAll(spec,
                PageRequest.of(safePage - 1, safeSize,
                        Sort.by(Sort.Direction.ASC, "plateNumber", "startTime")));
        List<WhitelistVehicleView> items = result.getContent().stream().map(WhitelistVehicleView::from).toList();
        return PageResult.of(items, result.getTotalElements(), safePage, safeSize);
    }

    @Transactional
    public WhitelistVehicleView createVehicle(Long lotId, CreateWhitelistVehicleRequest request) {
        adminGuard.requireEnabledAdmin();
        ParkingLot lot = requireLot(lotId);
        String plateNumber = requireText(request == null ? null : request.plateNumber(), 20);
        String ownerName = requireText(request.ownerName(), 80);
        PlateColor color = request.plateColor() == null ? PlateColor.BLUE : request.plateColor();
        VehicleType type = request.type() == null ? VehicleType.OTHER : request.type();
        LocalDateTime startTime = requireTimeRange(request.startTime(), request.endTime());
        boolean enabled = request.enabled() == null || request.enabled();

        WhitelistVehicle vehicle = new WhitelistVehicle();
        vehicle.setLot(lot);
        vehicle.setPlateNumber(plateNumber);
        vehicle.setPlateColor(color);
        vehicle.setOwnerName(ownerName);
        vehicle.setType(type);
        vehicle.setPhone(normalizeOptional(request.phone()));
        vehicle.setDepartment(normalizeOptional(request.department()));
        vehicle.setRemark(normalizeOptional(request.remark()));
        vehicle.setStartTime(startTime);
        vehicle.setEndTime(request.endTime());
        vehicle.setEnabled(enabled);
        WhitelistVehicle saved = vehicles.save(vehicle);
        notifier.upsert(EdgeConfigSyncProtocol.DOMAIN_WHITELIST, lot.getCode(), saved);
        return WhitelistVehicleView.from(saved);
    }

    @Transactional
    public WhitelistVehicleView updateVehicle(Long lotId, Long vehicleId, UpdateWhitelistVehicleRequest request) {
        adminGuard.requireEnabledAdmin();
        ParkingLot lot = requireLot(lotId);
        WhitelistVehicle vehicle = requireVehicle(vehicleId);
        if (!vehicle.getLot().getId().equals(lotId)) {
            throw new BizException(404, MessageKeys.COMMON_NOT_FOUND);
        }
        String plateNumber = requireText(request == null ? null : request.plateNumber(), 20);
        String ownerName = requireText(request.ownerName(), 80);
        PlateColor color = request.plateColor() == null ? vehicle.getPlateColor() : request.plateColor();
        VehicleType type = request.type() == null ? vehicle.getType() : request.type();
        LocalDateTime startTime = requireTimeRange(request.startTime(), request.endTime());
        boolean enabled = request.enabled() == null ? vehicle.isEnabled() : request.enabled();
        vehicle.setPlateNumber(plateNumber);
        vehicle.setPlateColor(color);
        vehicle.setOwnerName(ownerName);
        vehicle.setType(type);
        vehicle.setPhone(normalizeOptional(request.phone()));
        vehicle.setDepartment(normalizeOptional(request.department()));
        vehicle.setRemark(normalizeOptional(request.remark()));
        vehicle.setStartTime(startTime);
        vehicle.setEndTime(request.endTime());
        vehicle.setEnabled(enabled);
        WhitelistVehicle saved = vehicles.save(vehicle);
        notifier.upsert(EdgeConfigSyncProtocol.DOMAIN_WHITELIST, lot.getCode(), saved);
        return WhitelistVehicleView.from(saved);
    }

    @Transactional
    public void deleteVehicle(Long lotId, Long vehicleId) {
        adminGuard.requireEnabledAdmin();
        ParkingLot lot = requireLot(lotId);
        WhitelistVehicle vehicle = requireVehicle(vehicleId);
        if (!vehicle.getLot().getId().equals(lotId)) {
            throw new BizException(404, MessageKeys.COMMON_NOT_FOUND);
        }
        vehicles.delete(vehicle);
        notifier.delete(EdgeConfigSyncProtocol.DOMAIN_WHITELIST, lot.getCode(), vehicle.getId());
    }

    @Transactional
    public VehicleImportResult importVehicles(Long lotId, MultipartFile file) {
        adminGuard.requireEnabledAdmin();
        ParkingLot lot = requireLot(lotId);
        List<String[]> rows = ParkingSpreadsheetSupport.readRows(
                file, ParkingSpreadsheetSupport.WHITELIST_COLUMN_COUNT);
        int imported = 0;
        int skipped = 0;
        List<WhitelistVehicle> created = new ArrayList<>();
        for (String[] cells : rows) {
            String plate = ParkingSpreadsheetSupport.cell(cells, 0);
            String owner = ParkingSpreadsheetSupport.cell(cells, 1);
            if (plate.isEmpty() || plate.length() > MAX_PLATE_LENGTH
                    || owner.isEmpty() || owner.length() > MAX_OWNER_LENGTH) {
                skipped++;
                continue;
            }
            String colorToken = ParkingSpreadsheetSupport.cell(cells, 2);
            PlateColor color = colorToken.isEmpty()
                    ? PlateColor.BLUE
                    : ParkingSpreadsheetSupport.parsePlateColor(colorToken);
            // Excel 中的时间为「站点本地挂钟时间」，入库前换算成 UTC 锚点，与全库时间语义一致
            LocalDateTime siteStart = ParkingSpreadsheetSupport.parseDateTime(
                    ParkingSpreadsheetSupport.cell(cells, 6));
            LocalDateTime siteEnd = ParkingSpreadsheetSupport.parseDateTime(
                    ParkingSpreadsheetSupport.cell(cells, 7));
            if (siteStart == null || siteEnd == null || !siteEnd.isAfter(siteStart)) {
                skipped++;
                continue;
            }
            LocalDateTime startTime = SiteZoneTimes.toUtcAnchor(siteStart, siteZoneProvider.currentZone());
            LocalDateTime endTime = SiteZoneTimes.toUtcAnchor(siteEnd, siteZoneProvider.currentZone());
            VehicleType type = ParkingSpreadsheetSupport.parseVehicleType(
                    ParkingSpreadsheetSupport.cell(cells, 8));
            WhitelistVehicle vehicle = new WhitelistVehicle();
            vehicle.setLot(lot);
            vehicle.setPlateNumber(plate);
            vehicle.setPlateColor(color);
            vehicle.setOwnerName(owner);
            vehicle.setType(type);
            vehicle.setPhone(normalizeOptional(ParkingSpreadsheetSupport.cell(cells, 3)));
            vehicle.setDepartment(normalizeOptional(ParkingSpreadsheetSupport.cell(cells, 4)));
            vehicle.setRemark(normalizeOptional(ParkingSpreadsheetSupport.cell(cells, 5)));
            vehicle.setStartTime(startTime);
            vehicle.setEndTime(endTime);
            vehicle.setEnabled(true);
            created.add(vehicles.save(vehicle));
            imported++;
        }
        if (!created.isEmpty()) {
            notifier.upserts(EdgeConfigSyncProtocol.DOMAIN_WHITELIST, lot.getCode(), created);
        }
        return new VehicleImportResult(null, imported, skipped);
    }

    @Transactional(readOnly = true)
    public byte[] buildImportTemplate(Long lotId) {
        requireLot(lotId);
        return ParkingSpreadsheetSupport.buildTemplate(
                "白名单", ParkingSpreadsheetSupport.WHITELIST_TEMPLATE_COLUMNS);
    }

    @Transactional(readOnly = true)
    public byte[] exportVehicles(Long lotId, String plate) {
        requireLot(lotId);
        String trimmedPlate = StringUtils.hasText(plate) ? plate.trim() : null;
        List<WhitelistVehicle> result = vehicles.findAll(buildSpec(lotId, trimmedPlate),
                Sort.by(Sort.Direction.ASC, "plateNumber", "startTime"));
        List<String[]> rows = new ArrayList<>();
        for (WhitelistVehicle v : result) {
            rows.add(new String[] {
                    v.getPlateNumber(),
                    v.getOwnerName(),
                    v.getPlateColor().name(),
                    nullToEmpty(v.getPhone()),
                    nullToEmpty(v.getDepartment()),
                    nullToEmpty(v.getRemark()),
                    // 库内为 UTC 锚点，导出前换算回「站点本地挂钟时间」再输出
                    v.getStartTime() == null ? "" : SiteZoneTimes.toSiteWall(
                            v.getStartTime(), siteZoneProvider.currentZone()).format(EXPORT_TIME_FORMATTER),
                    v.getEndTime() == null ? "" : SiteZoneTimes.toSiteWall(
                            v.getEndTime(), siteZoneProvider.currentZone()).format(EXPORT_TIME_FORMATTER),
                    v.getType().name(),
            });
        }
        return ParkingSpreadsheetSupport.buildExport(
                "白名单", ParkingSpreadsheetSupport.WHITELIST_TEMPLATE_COLUMNS, rows);
    }

    private Specification<WhitelistVehicle> buildSpec(Long lotId, String plate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("lot").get("id"), lotId));
            if (plate != null && !plate.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("plateNumber")), "%" + plate.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private LocalDateTime requireTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new BizException(400, MessageKeys.PARKING_WHITELIST_VEHICLE_INVALID_TIME_RANGE);
        }
        return startTime;
    }

    private ParkingLot requireLot(Long lotId) {
        return lots.findById(lotId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
    }

    private WhitelistVehicle requireVehicle(Long vehicleId) {
        return vehicles.findById(vehicleId)
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

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

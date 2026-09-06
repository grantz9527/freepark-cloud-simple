package com.freepark.cloud.simple.parking.service;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.parking.dto.CreateInternalVehicleRequest;
import com.freepark.cloud.simple.parking.dto.InternalVehicleView;
import com.freepark.cloud.simple.parking.dto.UpdateInternalVehicleRequest;
import com.freepark.cloud.simple.parking.dto.VehicleImportResult;
import com.freepark.cloud.simple.parking.entity.InternalVehicle;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.entity.PlateColor;
import com.freepark.cloud.simple.parking.entity.VehicleType;
import com.freepark.cloud.simple.parking.repository.InternalVehicleRepository;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.parking.support.ParkingSpreadsheetSupport;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 内部车辆服务：列表/增改删/Excel 导入导出/按批次删除。
 */
@Service
public class InternalVehicleService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_PLATE_LENGTH = 20;
    private static final int MAX_OWNER_LENGTH = 80;

    private final ParkingLotRepository lots;
    private final InternalVehicleRepository vehicles;
    private final AdminGuard adminGuard;

    public InternalVehicleService(ParkingLotRepository lots,
                                  InternalVehicleRepository vehicles,
                                  AdminGuard adminGuard) {
        this.lots = lots;
        this.vehicles = vehicles;
        this.adminGuard = adminGuard;
    }

    @Transactional(readOnly = true)
    public PageResult<InternalVehicleView> listVehicles(Long lotId, String plate, int page, int size) {
        requireLot(lotId);
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String trimmedPlate = StringUtils.hasText(plate) ? plate.trim() : null;
        Specification<InternalVehicle> spec = buildSpec(lotId, trimmedPlate);
        Page<InternalVehicle> result = vehicles.findAll(spec,
                PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.ASC, "plateNumber")));
        List<InternalVehicleView> items = result.getContent().stream().map(InternalVehicleView::from).toList();
        return PageResult.of(items, result.getTotalElements(), safePage, safeSize);
    }

    @Transactional
    public InternalVehicleView createVehicle(Long lotId, CreateInternalVehicleRequest request) {
        adminGuard.requireEnabledAdmin();
        ParkingLot lot = requireLot(lotId);
        String plateNumber = requireText(request == null ? null : request.plateNumber(), 20);
        if (vehicles.existsByLotIdAndPlateNumberIgnoreCase(lotId, plateNumber)) {
            throw new BizException(400, MessageKeys.PARKING_INTERNAL_VEHICLE_PLATE_EXISTS);
        }
        String ownerName = requireText(request.ownerName(), 80);
        PlateColor color = request.plateColor() == null ? PlateColor.BLUE : request.plateColor();
        VehicleType type = request.type() == null ? VehicleType.OTHER : request.type();
        boolean enabled = request.enabled() == null || request.enabled();

        InternalVehicle vehicle = new InternalVehicle();
        vehicle.setLot(lot);
        vehicle.setPlateNumber(plateNumber);
        vehicle.setPlateColor(color);
        vehicle.setOwnerName(ownerName);
        vehicle.setType(type);
        vehicle.setPhone(normalizeOptional(request.phone()));
        vehicle.setDepartment(normalizeOptional(request.department()));
        vehicle.setRemark(normalizeOptional(request.remark()));
        vehicle.setEnabled(enabled);
        return InternalVehicleView.from(vehicles.save(vehicle));
    }

    @Transactional
    public InternalVehicleView updateVehicle(Long lotId, Long vehicleId, UpdateInternalVehicleRequest request) {
        adminGuard.requireEnabledAdmin();
        requireLot(lotId);
        InternalVehicle vehicle = requireVehicle(vehicleId);
        if (!vehicle.getLot().getId().equals(lotId)) {
            throw new BizException(404, MessageKeys.COMMON_NOT_FOUND);
        }
        String plateNumber = requireText(request == null ? null : request.plateNumber(), 20);
        if (vehicles.existsByLotIdAndPlateNumberIgnoreCaseAndIdNot(lotId, plateNumber, vehicleId)) {
            throw new BizException(400, MessageKeys.PARKING_INTERNAL_VEHICLE_PLATE_EXISTS);
        }
        String ownerName = requireText(request.ownerName(), 80);
        PlateColor color = request.plateColor() == null ? vehicle.getPlateColor() : request.plateColor();
        VehicleType type = request.type() == null ? vehicle.getType() : request.type();
        boolean enabled = request.enabled() == null ? vehicle.isEnabled() : request.enabled();
        vehicle.setPlateNumber(plateNumber);
        vehicle.setPlateColor(color);
        vehicle.setOwnerName(ownerName);
        vehicle.setType(type);
        vehicle.setPhone(normalizeOptional(request.phone()));
        vehicle.setDepartment(normalizeOptional(request.department()));
        vehicle.setRemark(normalizeOptional(request.remark()));
        vehicle.setEnabled(enabled);
        return InternalVehicleView.from(vehicles.save(vehicle));
    }

    @Transactional
    public void deleteVehicle(Long lotId, Long vehicleId) {
        adminGuard.requireEnabledAdmin();
        requireLot(lotId);
        InternalVehicle vehicle = requireVehicle(vehicleId);
        if (!vehicle.getLot().getId().equals(lotId)) {
            throw new BizException(404, MessageKeys.COMMON_NOT_FOUND);
        }
        vehicles.delete(vehicle);
    }

    @Transactional
    public VehicleImportResult importVehicles(Long lotId, MultipartFile file) {
        adminGuard.requireEnabledAdmin();
        ParkingLot lot = requireLot(lotId);
        List<String[]> rows = ParkingSpreadsheetSupport.readRows(
                file, ParkingSpreadsheetSupport.INTERNAL_COLUMN_COUNT);
        String batchId = UUID.randomUUID().toString();
        int imported = 0;
        int skipped = 0;
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
            VehicleType type = ParkingSpreadsheetSupport.parseVehicleType(
                    ParkingSpreadsheetSupport.cell(cells, 6));
            if (vehicles.existsByLotIdAndPlateNumberIgnoreCase(lotId, plate)) {
                skipped++;
                continue;
            }
            InternalVehicle vehicle = new InternalVehicle();
            vehicle.setLot(lot);
            vehicle.setPlateNumber(plate);
            vehicle.setPlateColor(color);
            vehicle.setOwnerName(owner);
            vehicle.setType(type);
            vehicle.setPhone(normalizeOptional(ParkingSpreadsheetSupport.cell(cells, 3)));
            vehicle.setDepartment(normalizeOptional(ParkingSpreadsheetSupport.cell(cells, 4)));
            vehicle.setRemark(normalizeOptional(ParkingSpreadsheetSupport.cell(cells, 5)));
            vehicle.setBatchId(batchId);
            vehicle.setEnabled(true);
            vehicles.save(vehicle);
            imported++;
        }
        return new VehicleImportResult(batchId, imported, skipped);
    }

    @Transactional(readOnly = true)
    public byte[] buildImportTemplate(Long lotId) {
        requireLot(lotId);
        return ParkingSpreadsheetSupport.buildTemplate(
                "内部车辆", ParkingSpreadsheetSupport.INTERNAL_TEMPLATE_COLUMNS);
    }

    @Transactional(readOnly = true)
    public byte[] exportVehicles(Long lotId, String plate) {
        requireLot(lotId);
        String trimmedPlate = StringUtils.hasText(plate) ? plate.trim() : null;
        List<InternalVehicle> result = vehicles.findAll(buildSpec(lotId, trimmedPlate),
                Sort.by(Sort.Direction.ASC, "plateNumber"));
        List<String[]> rows = new ArrayList<>();
        for (InternalVehicle v : result) {
            rows.add(new String[] {
                    v.getPlateNumber(),
                    v.getOwnerName(),
                    v.getPlateColor().name(),
                    nullToEmpty(v.getPhone()),
                    nullToEmpty(v.getDepartment()),
                    nullToEmpty(v.getRemark()),
                    v.getType().name(),
            });
        }
        return ParkingSpreadsheetSupport.buildExport(
                "内部车辆", ParkingSpreadsheetSupport.INTERNAL_TEMPLATE_COLUMNS, rows);
    }

    @Transactional
    public int deleteVehiclesByBatch(Long lotId, String batchId) {
        adminGuard.requireEnabledAdmin();
        requireLot(lotId);
        if (!StringUtils.hasText(batchId)) {
            throw new BizException(404, MessageKeys.COMMON_NOT_FOUND);
        }
        List<InternalVehicle> batch = vehicles.findAllByLotIdAndBatchId(lotId, batchId);
        if (batch.isEmpty()) {
            throw new BizException(404, MessageKeys.COMMON_NOT_FOUND);
        }
        vehicles.deleteAll(batch);
        return batch.size();
    }

    private Specification<InternalVehicle> buildSpec(Long lotId, String plate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("lot").get("id"), lotId));
            if (plate != null && !plate.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("plateNumber")), "%" + plate.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private ParkingLot requireLot(Long lotId) {
        return lots.findById(lotId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
    }

    private InternalVehicle requireVehicle(Long vehicleId) {
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

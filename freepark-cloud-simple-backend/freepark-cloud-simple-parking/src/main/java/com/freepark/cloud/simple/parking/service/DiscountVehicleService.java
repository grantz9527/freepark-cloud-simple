package com.freepark.cloud.simple.parking.service;

import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.parking.dto.CreateDiscountVehicleRequest;
import com.freepark.cloud.simple.parking.dto.DiscountVehicleView;
import com.freepark.cloud.simple.parking.dto.UpdateDiscountVehicleRequest;
import com.freepark.cloud.simple.parking.dto.VehicleImportResult;
import com.freepark.cloud.simple.parking.entity.DiscountVehicle;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.entity.PlateColor;
import com.freepark.cloud.simple.parking.repository.DiscountVehicleRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 优惠车辆（指定车辆每次入场免费时长）服务。
 * <p>
 * 同一车场同一车牌仅保留一条记录；启用后该车每次入场自入场时刻起免费停
 * {@code freeMinutes} 分钟，超出部分按车场计费模板正常计费。列表/导入导出与其它名单风格一致。
 * 该配置仅影响云端结算，不参与边缘名单同步。
 * </p>
 */
@Service
public class DiscountVehicleService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_PLATE_LENGTH = 20;
    private static final int MAX_REMARK_LENGTH = 255;
    private static final int MAX_FREE_MINUTES = 10080;
    private static final int COLUMN_COUNT = 4;

    private static final String[] TEMPLATE_COLUMNS = {
            "车牌号", "车牌颜色", "免费时长(分钟)", "备注"
    };

    private final ParkingLotRepository lots;
    private final DiscountVehicleRepository vehicles;
    private final AdminGuard adminGuard;

    public DiscountVehicleService(ParkingLotRepository lots,
                                  DiscountVehicleRepository vehicles,
                                  AdminGuard adminGuard) {
        this.lots = lots;
        this.vehicles = vehicles;
        this.adminGuard = adminGuard;
    }

    @Transactional(readOnly = true)
    public PageResult<DiscountVehicleView> listVehicles(Long lotId, String plate, int page, int size) {
        requireLot(lotId);
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String trimmedPlate = StringUtils.hasText(plate) ? plate.trim() : null;
        Specification<DiscountVehicle> spec = buildSpec(lotId, trimmedPlate);
        Page<DiscountVehicle> result = vehicles.findAll(spec,
                PageRequest.of(safePage - 1, safeSize,
                        Sort.by(Sort.Direction.ASC, "plateNumber", "id")));
        List<DiscountVehicleView> items = result.getContent().stream()
                .map(DiscountVehicleView::from).toList();
        return PageResult.of(items, result.getTotalElements(), safePage, safeSize);
    }

    @Transactional
    public DiscountVehicleView createVehicle(Long lotId, CreateDiscountVehicleRequest request) {
        adminGuard.requireEnabledAdmin();
        ParkingLot lot = requireLot(lotId);
        if (request == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        String plateNumber = requirePlate(request.plateNumber());
        int freeMinutes = requireFreeMinutes(request.freeMinutes());
        PlateColor color = request.plateColor() == null ? PlateColor.BLUE : request.plateColor();
        assertPlateUnique(lotId, plateNumber, null);

        DiscountVehicle vehicle = new DiscountVehicle();
        vehicle.setLot(lot);
        vehicle.setPlateNumber(plateNumber);
        vehicle.setPlateColor(color);
        vehicle.setFreeMinutes(freeMinutes);
        vehicle.setRemark(normalizeOptional(request.remark()));
        vehicle.setEnabled(true);
        return DiscountVehicleView.from(vehicles.save(vehicle));
    }

    @Transactional
    public DiscountVehicleView updateVehicle(Long lotId, Long vehicleId,
                                             UpdateDiscountVehicleRequest request) {
        adminGuard.requireEnabledAdmin();
        ParkingLot lot = requireLot(lotId);
        DiscountVehicle vehicle = requireVehicle(vehicleId);
        if (!vehicle.getLot().getId().equals(lotId)) {
            throw new BizException(404, MessageKeys.COMMON_NOT_FOUND);
        }
        if (request == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        String plateNumber = request.plateNumber() == null || request.plateNumber().isBlank()
                ? vehicle.getPlateNumber()
                : requirePlate(request.plateNumber());
        int freeMinutes = request.freeMinutes() == null
                ? vehicle.getFreeMinutes()
                : requireFreeMinutes(request.freeMinutes());
        PlateColor color = request.plateColor() == null ? vehicle.getPlateColor() : request.plateColor();
        boolean enabled = request.enabled() == null ? vehicle.isEnabled() : request.enabled();
        if (!vehicle.getPlateNumber().equalsIgnoreCase(plateNumber)) {
            assertPlateUnique(lotId, plateNumber, vehicleId);
        }
        vehicle.setPlateNumber(plateNumber);
        vehicle.setPlateColor(color);
        vehicle.setFreeMinutes(freeMinutes);
        vehicle.setRemark(normalizeOptional(request.remark()));
        vehicle.setEnabled(enabled);
        return DiscountVehicleView.from(vehicles.save(vehicle));
    }

    @Transactional
    public void deleteVehicle(Long lotId, Long vehicleId) {
        adminGuard.requireEnabledAdmin();
        DiscountVehicle vehicle = requireVehicle(vehicleId);
        if (!vehicle.getLot().getId().equals(lotId)) {
            throw new BizException(404, MessageKeys.COMMON_NOT_FOUND);
        }
        vehicles.delete(vehicle);
    }

    @Transactional
    public VehicleImportResult importVehicles(Long lotId, MultipartFile file) {
        adminGuard.requireEnabledAdmin();
        ParkingLot lot = requireLot(lotId);
        List<String[]> rows = ParkingSpreadsheetSupport.readRows(file, COLUMN_COUNT);
        int imported = 0;
        int skipped = 0;
        Set<String> seenPlates = new HashSet<>();
        List<DiscountVehicle> created = new ArrayList<>();
        for (String[] cells : rows) {
            String plate = normalizePlate(ParkingSpreadsheetSupport.cell(cells, 0));
            if (plate == null) {
                skipped++;
                continue;
            }
            String minutesToken = ParkingSpreadsheetSupport.cell(cells, 2);
            int freeMinutes;
            try {
                freeMinutes = Integer.parseInt(minutesToken);
            } catch (NumberFormatException e) {
                freeMinutes = 0;
            }
            if (freeMinutes < 1 || freeMinutes > MAX_FREE_MINUTES) {
                skipped++;
                continue;
            }
            String lower = plate.toLowerCase(Locale.ROOT);
            if (!seenPlates.add(lower)
                    || !vehicles.findAllByLotIdAndPlateNumberIgnoreCase(lotId, plate).isEmpty()) {
                skipped++;
                continue;
            }
            String colorToken = ParkingSpreadsheetSupport.cell(cells, 1);
            PlateColor color = colorToken.isEmpty()
                    ? PlateColor.BLUE
                    : ParkingSpreadsheetSupport.parsePlateColor(colorToken);
            DiscountVehicle vehicle = new DiscountVehicle();
            vehicle.setLot(requireLot(lotId));
            vehicle.setPlateNumber(plate);
            vehicle.setPlateColor(color);
            vehicle.setFreeMinutes(freeMinutes);
            vehicle.setRemark(normalizeOptional(ParkingSpreadsheetSupport.cell(cells, 3)));
            vehicle.setEnabled(true);
            vehicles.save(vehicle);
            imported++;
        }
        return new VehicleImportResult(null, imported, skipped);
    }

    @Transactional(readOnly = true)
    public byte[] buildImportTemplate(Long lotId) {
        requireLot(lotId);
        return ParkingSpreadsheetSupport.buildTemplate("优惠车辆", TEMPLATE_COLUMNS);
    }

    @Transactional(readOnly = true)
    public byte[] exportVehicles(Long lotId, String plate) {
        requireLot(lotId);
        String trimmedPlate = StringUtils.hasText(plate) ? plate.trim() : null;
        List<DiscountVehicle> result = vehicles.findAll(buildSpec(lotId, trimmedPlate),
                Sort.by(Sort.Direction.ASC, "plateNumber", "id"));
        List<String[]> rows = new ArrayList<>();
        for (DiscountVehicle v : result) {
            rows.add(new String[] {
                    v.getPlateNumber(),
                    v.getPlateColor().name(),
                    String.valueOf(v.getFreeMinutes()),
                    v.getRemark() == null ? "" : v.getRemark()
            });
        }
        return ParkingSpreadsheetSupport.buildExport("优惠车辆", TEMPLATE_COLUMNS, rows);
    }

    private Specification<DiscountVehicle> buildSpec(Long lotId, String plate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("lot").get("id"), lotId));
            if (plate != null && !plate.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("plateNumber")),
                        "%" + plate.toLowerCase(Locale.ROOT) + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void assertPlateUnique(Long lotId, String plate, Long excludeId) {
        for (DiscountVehicle existing : vehicles.findAllByLotIdAndPlateNumberIgnoreCase(lotId, plate)) {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
            }
        }
    }

    private ParkingLot requireLot(Long lotId) {
        return lots.findById(lotId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
    }

    private DiscountVehicle requireVehicle(Long vehicleId) {
        return vehicles.findById(vehicleId)
                .orElseThrow(() -> new BizException(404, MessageKeys.COMMON_NOT_FOUND));
    }

    private String requirePlate(String value) {
        String normalized = normalizePlate(value);
        if (normalized == null) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizePlate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim().toUpperCase(Locale.ROOT);
        return trimmed.length() > MAX_PLATE_LENGTH ? null : trimmed;
    }

    private int requireFreeMinutes(Integer value) {
        if (value == null || value < 1 || value > MAX_FREE_MINUTES) {
            throw new BizException(400, MessageKeys.COMMON_BAD_REQUEST);
        }
        return value;
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > MAX_REMARK_LENGTH ? trimmed.substring(0, MAX_REMARK_LENGTH) : trimmed;
    }
}

package com.freepark.cloud.simple.parking.web;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.parking.dto.CreateDiscountVehicleRequest;
import com.freepark.cloud.simple.parking.dto.DiscountVehicleView;
import com.freepark.cloud.simple.parking.dto.UpdateDiscountVehicleRequest;
import com.freepark.cloud.simple.parking.dto.VehicleImportResult;
import com.freepark.cloud.simple.parking.service.DiscountVehicleService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 优惠车辆管理（指定车辆每次入场免费时长）。
 */
@RestController
@RequestMapping("/api/lots/{lotId}/discount-vehicles")
public class DiscountVehicleController {

    private static final String EXCEL_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final DiscountVehicleService discountVehicleService;

    public DiscountVehicleController(DiscountVehicleService discountVehicleService) {
        this.discountVehicleService = discountVehicleService;
    }

    @GetMapping
    public ApiResult<PageResult<DiscountVehicleView>> list(@PathVariable Long lotId,
                                                           @RequestParam(required = false) String plate,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        return ApiResult.ok(discountVehicleService.listVehicles(lotId, plate, page, size));
    }

    @PostMapping
    public ApiResult<DiscountVehicleView> create(@PathVariable Long lotId,
                                                 @RequestBody CreateDiscountVehicleRequest request) {
        return ApiResult.ok(discountVehicleService.createVehicle(lotId, request));
    }

    @PutMapping("/{vehicleId}")
    public ApiResult<DiscountVehicleView> update(@PathVariable Long lotId,
                                                 @PathVariable Long vehicleId,
                                                 @RequestBody UpdateDiscountVehicleRequest request) {
        return ApiResult.ok(discountVehicleService.updateVehicle(lotId, vehicleId, request));
    }

    @DeleteMapping("/{vehicleId}")
    public ApiResult<Void> delete(@PathVariable Long lotId,
                                  @PathVariable Long vehicleId) {
        discountVehicleService.deleteVehicle(lotId, vehicleId);
        return ApiResult.ok();
    }

    @GetMapping("/import-template")
    public ResponseEntity<byte[]> downloadImportTemplate(@PathVariable Long lotId) {
        byte[] body = discountVehicleService.buildImportTemplate(lotId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"discount-template.xlsx\"")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(body);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportVehicles(@PathVariable Long lotId,
                                                 @RequestParam(required = false) String plate) {
        byte[] body = discountVehicleService.exportVehicles(lotId, plate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"discount-vehicles.xlsx\"")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(body);
    }

    @PostMapping("/import")
    public ApiResult<VehicleImportResult> importVehicles(@PathVariable Long lotId,
                                                         @RequestParam("file") MultipartFile file) {
        return ApiResult.ok(discountVehicleService.importVehicles(lotId, file));
    }
}

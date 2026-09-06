package com.freepark.cloud.simple.parking.web;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.parking.dto.BlacklistVehicleView;
import com.freepark.cloud.simple.parking.dto.CreateBlacklistVehicleRequest;
import com.freepark.cloud.simple.parking.dto.UpdateBlacklistVehicleRequest;
import com.freepark.cloud.simple.parking.dto.VehicleImportResult;
import com.freepark.cloud.simple.parking.service.BlacklistVehicleService;
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
 * 黑名单车辆管理。
 */
@RestController
@RequestMapping("/api/lots/{lotId}/blacklist-vehicles")
public class BlacklistVehicleController {

    private static final String EXCEL_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final BlacklistVehicleService blacklistVehicleService;

    public BlacklistVehicleController(BlacklistVehicleService blacklistVehicleService) {
        this.blacklistVehicleService = blacklistVehicleService;
    }

    @GetMapping
    public ApiResult<PageResult<BlacklistVehicleView>> list(@PathVariable Long lotId,
                                                            @RequestParam(required = false) String plate,
                                                            @RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        return ApiResult.ok(blacklistVehicleService.listVehicles(lotId, plate, page, size));
    }

    @PostMapping
    public ApiResult<BlacklistVehicleView> create(@PathVariable Long lotId,
                                                  @RequestBody CreateBlacklistVehicleRequest request) {
        return ApiResult.ok(blacklistVehicleService.createVehicle(lotId, request));
    }

    @PutMapping("/{vehicleId}")
    public ApiResult<BlacklistVehicleView> update(@PathVariable Long lotId,
                                                  @PathVariable Long vehicleId,
                                                  @RequestBody UpdateBlacklistVehicleRequest request) {
        return ApiResult.ok(blacklistVehicleService.updateVehicle(lotId, vehicleId, request));
    }

    @DeleteMapping("/{vehicleId}")
    public ApiResult<Void> delete(@PathVariable Long lotId,
                                  @PathVariable Long vehicleId) {
        blacklistVehicleService.deleteVehicle(lotId, vehicleId);
        return ApiResult.ok();
    }

    @GetMapping("/import-template")
    public ResponseEntity<byte[]> downloadImportTemplate(@PathVariable Long lotId) {
        byte[] body = blacklistVehicleService.buildImportTemplate(lotId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"blacklist-template.xlsx\"")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(body);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportVehicles(@PathVariable Long lotId,
                                                 @RequestParam(required = false) String plate) {
        byte[] body = blacklistVehicleService.exportVehicles(lotId, plate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"blacklist-vehicles.xlsx\"")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(body);
    }

    @PostMapping("/import")
    public ApiResult<VehicleImportResult> importVehicles(@PathVariable Long lotId,
                                                         @RequestParam("file") MultipartFile file) {
        return ApiResult.ok(blacklistVehicleService.importVehicles(lotId, file));
    }
}

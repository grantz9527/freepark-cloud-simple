package com.freepark.cloud.simple.parking.web;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.parking.dto.CreateWhitelistVehicleRequest;
import com.freepark.cloud.simple.parking.dto.UpdateWhitelistVehicleRequest;
import com.freepark.cloud.simple.parking.dto.VehicleImportResult;
import com.freepark.cloud.simple.parking.dto.WhitelistVehicleView;
import com.freepark.cloud.simple.parking.service.WhitelistVehicleService;
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
 * 白名单车辆（停车卡）管理。
 */
@RestController
@RequestMapping("/api/lots/{lotId}/whitelist-vehicles")
public class WhitelistVehicleController {

    private static final String EXCEL_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final WhitelistVehicleService whitelistVehicleService;

    public WhitelistVehicleController(WhitelistVehicleService whitelistVehicleService) {
        this.whitelistVehicleService = whitelistVehicleService;
    }

    @GetMapping
    public ApiResult<PageResult<WhitelistVehicleView>> list(@PathVariable Long lotId,
                                                            @RequestParam(required = false) String plate,
                                                            @RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        return ApiResult.ok(whitelistVehicleService.listVehicles(lotId, plate, page, size));
    }

    @PostMapping
    public ApiResult<WhitelistVehicleView> create(@PathVariable Long lotId,
                                                  @RequestBody CreateWhitelistVehicleRequest request) {
        return ApiResult.ok(whitelistVehicleService.createVehicle(lotId, request));
    }

    @PutMapping("/{vehicleId}")
    public ApiResult<WhitelistVehicleView> update(@PathVariable Long lotId,
                                                  @PathVariable Long vehicleId,
                                                  @RequestBody UpdateWhitelistVehicleRequest request) {
        return ApiResult.ok(whitelistVehicleService.updateVehicle(lotId, vehicleId, request));
    }

    @DeleteMapping("/{vehicleId}")
    public ApiResult<Void> delete(@PathVariable Long lotId,
                                  @PathVariable Long vehicleId) {
        whitelistVehicleService.deleteVehicle(lotId, vehicleId);
        return ApiResult.ok();
    }

    @GetMapping("/import-template")
    public ResponseEntity<byte[]> downloadImportTemplate(@PathVariable Long lotId) {
        byte[] body = whitelistVehicleService.buildImportTemplate(lotId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"whitelist-template.xlsx\"")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(body);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportVehicles(@PathVariable Long lotId,
                                                 @RequestParam(required = false) String plate) {
        byte[] body = whitelistVehicleService.exportVehicles(lotId, plate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"whitelist-vehicles.xlsx\"")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(body);
    }

    @PostMapping("/import")
    public ApiResult<VehicleImportResult> importVehicles(@PathVariable Long lotId,
                                                         @RequestParam("file") MultipartFile file) {
        return ApiResult.ok(whitelistVehicleService.importVehicles(lotId, file));
    }
}

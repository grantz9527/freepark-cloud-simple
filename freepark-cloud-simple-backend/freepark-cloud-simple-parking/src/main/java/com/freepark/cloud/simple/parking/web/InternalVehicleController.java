package com.freepark.cloud.simple.parking.web;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.parking.dto.CreateInternalVehicleRequest;
import com.freepark.cloud.simple.parking.dto.InternalVehicleView;
import com.freepark.cloud.simple.parking.dto.UpdateInternalVehicleRequest;
import com.freepark.cloud.simple.parking.dto.VehicleImportResult;
import com.freepark.cloud.simple.parking.service.InternalVehicleService;
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
 * 内部车辆管理。
 */
@RestController
@RequestMapping("/api/lots/{lotId}/internal-vehicles")
public class InternalVehicleController {

    private static final String EXCEL_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final InternalVehicleService internalVehicleService;

    public InternalVehicleController(InternalVehicleService internalVehicleService) {
        this.internalVehicleService = internalVehicleService;
    }

    @GetMapping
    public ApiResult<PageResult<InternalVehicleView>> list(@PathVariable Long lotId,
                                                           @RequestParam(required = false) String plate,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        return ApiResult.ok(internalVehicleService.listVehicles(lotId, plate, page, size));
    }

    @PostMapping
    public ApiResult<InternalVehicleView> create(@PathVariable Long lotId,
                                                 @RequestBody CreateInternalVehicleRequest request) {
        return ApiResult.ok(internalVehicleService.createVehicle(lotId, request));
    }

    @PutMapping("/{vehicleId}")
    public ApiResult<InternalVehicleView> update(@PathVariable Long lotId,
                                                 @PathVariable Long vehicleId,
                                                 @RequestBody UpdateInternalVehicleRequest request) {
        return ApiResult.ok(internalVehicleService.updateVehicle(lotId, vehicleId, request));
    }

    @DeleteMapping("/{vehicleId}")
    public ApiResult<Void> delete(@PathVariable Long lotId,
                                  @PathVariable Long vehicleId) {
        internalVehicleService.deleteVehicle(lotId, vehicleId);
        return ApiResult.ok();
    }

    @DeleteMapping("/batch/{batchId}")
    public ApiResult<Integer> deleteBatch(@PathVariable Long lotId,
                                          @PathVariable String batchId) {
        return ApiResult.ok(internalVehicleService.deleteVehiclesByBatch(lotId, batchId));
    }

    @GetMapping("/import-template")
    public ResponseEntity<byte[]> downloadImportTemplate(@PathVariable Long lotId) {
        byte[] body = internalVehicleService.buildImportTemplate(lotId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"internal-vehicles-template.xlsx\"")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(body);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportVehicles(@PathVariable Long lotId,
                                                 @RequestParam(required = false) String plate) {
        byte[] body = internalVehicleService.exportVehicles(lotId, plate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"internal-vehicles.xlsx\"")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(body);
    }

    @PostMapping("/import")
    public ApiResult<VehicleImportResult> importVehicles(@PathVariable Long lotId,
                                                         @RequestParam("file") MultipartFile file) {
        return ApiResult.ok(internalVehicleService.importVehicles(lotId, file));
    }
}

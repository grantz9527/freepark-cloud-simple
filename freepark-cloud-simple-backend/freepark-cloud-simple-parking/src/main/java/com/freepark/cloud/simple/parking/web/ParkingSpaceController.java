package com.freepark.cloud.simple.parking.web;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.parking.dto.AreaView;
import com.freepark.cloud.simple.parking.dto.CreateAreaRequest;
import com.freepark.cloud.simple.parking.dto.CreateLocationRequest;
import com.freepark.cloud.simple.parking.dto.CreateSpaceRequest;
import com.freepark.cloud.simple.parking.dto.LocationView;
import com.freepark.cloud.simple.parking.dto.SpaceView;
import com.freepark.cloud.simple.parking.dto.UpdateSpaceRequest;
import com.freepark.cloud.simple.parking.service.ParkingSpaceService;
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

import java.util.List;

/**
 * 车位管理：位置 -> 区域 -> 车位。
 */
@RestController
@RequestMapping("/api/lots/{lotId}")
public class ParkingSpaceController {

    private static final String EXCEL_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ParkingSpaceService parkingSpaceService;

    public ParkingSpaceController(ParkingSpaceService parkingSpaceService) {
        this.parkingSpaceService = parkingSpaceService;
    }

    @GetMapping("/locations")
    public ApiResult<List<LocationView>> listLocations(@PathVariable Long lotId) {
        return ApiResult.ok(parkingSpaceService.listLocations(lotId));
    }

    @PostMapping("/locations")
    public ApiResult<LocationView> createLocation(@PathVariable Long lotId,
                                                  @RequestBody CreateLocationRequest request) {
        return ApiResult.ok(parkingSpaceService.createLocation(lotId, request));
    }

    @GetMapping("/areas")
    public ApiResult<List<AreaView>> listAreas(@PathVariable Long lotId,
                                               @RequestParam(required = false) Long locationId) {
        return ApiResult.ok(parkingSpaceService.listAreas(lotId, locationId));
    }

    @PostMapping("/areas")
    public ApiResult<AreaView> createArea(@PathVariable Long lotId,
                                          @RequestBody CreateAreaRequest request) {
        return ApiResult.ok(parkingSpaceService.createArea(lotId, request));
    }

    @GetMapping("/spaces")
    public ApiResult<PageResult<SpaceView>> listSpaces(@PathVariable Long lotId,
                                                       @RequestParam(required = false) Long locationId,
                                                       @RequestParam(required = false) Long areaId,
                                                       @RequestParam(required = false) String code,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        return ApiResult.ok(parkingSpaceService.listSpaces(lotId, locationId, areaId, code, page, size));
    }

    @PostMapping("/spaces")
    public ApiResult<SpaceView> createSpace(@PathVariable Long lotId,
                                            @RequestBody CreateSpaceRequest request) {
        return ApiResult.ok(parkingSpaceService.createSpace(lotId, request));
    }

    @PutMapping("/spaces/{spaceId}")
    public ApiResult<SpaceView> updateSpace(@PathVariable Long lotId,
                                            @PathVariable Long spaceId,
                                            @RequestBody UpdateSpaceRequest request) {
        return ApiResult.ok(parkingSpaceService.updateSpace(lotId, spaceId, request));
    }

    @DeleteMapping("/spaces/{spaceId}")
    public ApiResult<Void> deleteSpace(@PathVariable Long lotId,
                                       @PathVariable Long spaceId) {
        parkingSpaceService.deleteSpace(lotId, spaceId);
        return ApiResult.ok();
    }

    @GetMapping("/spaces/import-template")
    public ResponseEntity<byte[]> downloadImportTemplate(@PathVariable Long lotId) {
        byte[] body = parkingSpaceService.buildImportTemplate(lotId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"spaces-template.xlsx\"")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(body);
    }

    @PostMapping("/spaces/import")
    public ApiResult<Integer> importSpaces(@PathVariable Long lotId,
                                           @RequestParam Long areaId,
                                           @RequestParam("file") MultipartFile file) {
        return ApiResult.ok(parkingSpaceService.importSpaces(lotId, areaId, file));
    }
}

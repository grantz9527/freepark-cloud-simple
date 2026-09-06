package com.freepark.cloud.simple.parking.web;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.parking.dto.BoothView;
import com.freepark.cloud.simple.parking.dto.CreateBoothRequest;
import com.freepark.cloud.simple.parking.dto.UpdateBoothRequest;
import com.freepark.cloud.simple.parking.service.ParkingBoothService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 岗亭管理。
 */
@RestController
@RequestMapping("/api/lots/{lotId}/booths")
public class ParkingBoothController {

    private final ParkingBoothService parkingBoothService;

    public ParkingBoothController(ParkingBoothService parkingBoothService) {
        this.parkingBoothService = parkingBoothService;
    }

    @GetMapping
    public ApiResult<PageResult<BoothView>> list(@PathVariable Long lotId,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return ApiResult.ok(parkingBoothService.listBooths(lotId, keyword, page, size));
    }

    @PostMapping
    public ApiResult<BoothView> create(@PathVariable Long lotId,
                                       @RequestBody CreateBoothRequest request) {
        return ApiResult.ok(parkingBoothService.createBooth(lotId, request));
    }

    @PutMapping("/{boothId}")
    public ApiResult<BoothView> update(@PathVariable Long lotId,
                                       @PathVariable Long boothId,
                                       @RequestBody UpdateBoothRequest request) {
        return ApiResult.ok(parkingBoothService.updateBooth(lotId, boothId, request));
    }

    @DeleteMapping("/{boothId}")
    public ApiResult<Void> delete(@PathVariable Long lotId,
                                  @PathVariable Long boothId) {
        parkingBoothService.deleteBooth(lotId, boothId);
        return ApiResult.ok();
    }
}

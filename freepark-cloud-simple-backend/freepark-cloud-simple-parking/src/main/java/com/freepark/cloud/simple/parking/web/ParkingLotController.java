package com.freepark.cloud.simple.parking.web;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.parking.dto.AccessJudgmentView;
import com.freepark.cloud.simple.parking.dto.CreateLotRequest;
import com.freepark.cloud.simple.parking.dto.LotInterceptView;
import com.freepark.cloud.simple.parking.dto.LotView;
import com.freepark.cloud.simple.parking.dto.UpdateAccessJudgmentRequest;
import com.freepark.cloud.simple.parking.dto.UpdateLotInterceptRequest;
import com.freepark.cloud.simple.parking.dto.UpdateLotRequest;
import com.freepark.cloud.simple.parking.service.ParkingLotService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 停车场管理。
 */
@RestController
@RequestMapping("/api/lots")
public class ParkingLotController {

    private final ParkingLotService parkingLotService;

    public ParkingLotController(ParkingLotService parkingLotService) {
        this.parkingLotService = parkingLotService;
    }

    @GetMapping
    public ApiResult<List<LotView>> list() {
        return ApiResult.ok(parkingLotService.listLots());
    }

    @PostMapping
    public ApiResult<LotView> create(@RequestBody CreateLotRequest request) {
        return ApiResult.ok(parkingLotService.createLot(request));
    }

    @PutMapping("/{lotId}")
    public ApiResult<LotView> update(@PathVariable Long lotId,
                                     @RequestBody UpdateLotRequest request) {
        return ApiResult.ok(parkingLotService.updateLot(lotId, request));
    }

    @GetMapping("/{lotId}/intercept")
    public ApiResult<LotInterceptView> getIntercept(@PathVariable Long lotId) {
        return ApiResult.ok(parkingLotService.getLotIntercept(lotId));
    }

    @PutMapping("/{lotId}/intercept")
    public ApiResult<LotInterceptView> updateIntercept(@PathVariable Long lotId,
                                                       @RequestBody UpdateLotInterceptRequest request) {
        return ApiResult.ok(parkingLotService.updateLotIntercept(lotId, request));
    }

    @GetMapping("/{lotId}/access-judgment")
    public ApiResult<AccessJudgmentView> getAccessJudgment(@PathVariable Long lotId) {
        return ApiResult.ok(parkingLotService.getAccessJudgment(lotId));
    }

    @PutMapping("/{lotId}/access-judgment")
    public ApiResult<AccessJudgmentView> updateAccessJudgment(@PathVariable Long lotId,
                                                              @RequestBody UpdateAccessJudgmentRequest request) {
        return ApiResult.ok(parkingLotService.updateAccessJudgment(lotId, request));
    }
}

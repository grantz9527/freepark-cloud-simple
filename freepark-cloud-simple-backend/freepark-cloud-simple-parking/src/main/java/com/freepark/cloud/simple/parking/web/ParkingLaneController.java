package com.freepark.cloud.simple.parking.web;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.parking.dto.CreateLaneRequest;
import com.freepark.cloud.simple.parking.dto.LaneView;
import com.freepark.cloud.simple.parking.dto.UpdateLaneRequest;
import com.freepark.cloud.simple.parking.service.ParkingLaneService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 通道管理。
 */
@RestController
@RequestMapping("/api/lanes")
public class ParkingLaneController {

    private final ParkingLaneService parkingLaneService;

    public ParkingLaneController(ParkingLaneService parkingLaneService) {
        this.parkingLaneService = parkingLaneService;
    }

    @GetMapping
    public ApiResult<List<LaneView>> list(@RequestParam(required = false) Long lotId) {
        return ApiResult.ok(parkingLaneService.listLanes(lotId));
    }

    @PostMapping
    public ApiResult<LaneView> create(@RequestBody CreateLaneRequest request) {
        return ApiResult.ok(parkingLaneService.createLane(request));
    }

    @PutMapping("/{laneId}")
    public ApiResult<LaneView> update(@PathVariable Long laneId,
                                      @RequestBody UpdateLaneRequest request) {
        return ApiResult.ok(parkingLaneService.updateLane(laneId, request));
    }
}

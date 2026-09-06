package com.freepark.cloud.simple.parking.web;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.parking.dto.CreateParkingSessionRequest;
import com.freepark.cloud.simple.parking.dto.ParkingSessionView;
import com.freepark.cloud.simple.parking.dto.UpdateParkingSessionRequest;
import com.freepark.cloud.simple.parking.entity.ParkingSessionStatus;
import com.freepark.cloud.simple.parking.service.ParkingSessionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 停车流水管理（停车管理 - 停车流水）。
 */
@RestController
@RequestMapping("/api/parking-sessions")
public class ParkingSessionController {

    private final ParkingSessionService parkingSessionService;

    public ParkingSessionController(ParkingSessionService parkingSessionService) {
        this.parkingSessionService = parkingSessionService;
    }

    @GetMapping
    public ApiResult<PageResult<ParkingSessionView>> list(
            @RequestParam(required = false) Long lotId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ParkingSessionStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResult.ok(parkingSessionService.listSessions(lotId, keyword, status, page, size));
    }

    @GetMapping("/has-open")
    public ApiResult<Boolean> hasOpen(@RequestParam Long lotId,
                                      @RequestParam String plateNumber) {
        return ApiResult.ok(parkingSessionService.hasOpenSession(lotId, plateNumber));
    }

    /** 手动新增在场流水（入场）。 */
    @PostMapping
    public ApiResult<ParkingSessionView> create(@RequestBody CreateParkingSessionRequest request) {
        return ApiResult.ok(parkingSessionService.createSession(request));
    }

    /** 编辑流水：可修正入场信息，在场流水传入出场信息即自动关场。 */
    @PutMapping("/{sessionId}")
    public ApiResult<ParkingSessionView> update(@PathVariable Long sessionId,
                                                @RequestBody UpdateParkingSessionRequest request) {
        return ApiResult.ok(parkingSessionService.updateSession(sessionId, request));
    }

    /** 作废流水。 */
    @PostMapping("/{sessionId}/void")
    public ApiResult<ParkingSessionView> voidSession(@PathVariable Long sessionId) {
        return ApiResult.ok(parkingSessionService.voidSession(sessionId));
    }
}

package com.freepark.cloud.simple.parking.web;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.parking.dto.CreateParkingSessionRequest;
import com.freepark.cloud.simple.parking.dto.ParkingSessionView;
import com.freepark.cloud.simple.parking.dto.PayStatusRequest;
import com.freepark.cloud.simple.parking.dto.UpdateParkingSessionRequest;
import com.freepark.cloud.simple.parking.entity.ParkingSessionStatus;
import com.freepark.cloud.simple.parking.service.ParkingSessionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResult.ok(parkingSessionService.listSessions(lotId, keyword, status, startDate, endDate, page, size));
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

    /**
     * 预览「重新算费」：只读计算应收金额但不落库，供前端弹窗确认；
     * 确认后调用 /recalculate 才真正快照生效，取消则无需任何落库操作。
     */
    @PostMapping("/{sessionId}/fee-preview")
    public ApiResult<BigDecimal> previewRecalculate(@PathVariable Long sessionId) {
        return ApiResult.ok(parkingSessionService.previewRecalculate(sessionId));
    }

    /**
     * 手动重新算费：已出场按真实出场时间、在场按「入场 ~ 当前时刻」估算，快照应收金额。
     */
    @PostMapping("/{sessionId}/recalculate")
    public ApiResult<ParkingSessionView> recalculate(@PathVariable Long sessionId) {
        return ApiResult.ok(parkingSessionService.recalculateSession(sessionId));
    }

    /** 人工登记支付状态（未支付/部分支付/已支付），仅已出场流水可登记；费用重算不自动改变它。 */
    @PostMapping("/{sessionId}/pay-status")
    public ApiResult<ParkingSessionView> markPayStatus(@PathVariable Long sessionId,
                                                       @RequestBody PayStatusRequest request) {
        return ApiResult.ok(parkingSessionService.markPayStatus(sessionId, request.status()));
    }
}

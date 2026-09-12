package com.freepark.cloud.simple.parking.web;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.common.web.PageResult;
import com.freepark.cloud.simple.parking.dto.ParkingOrderRefundView;
import com.freepark.cloud.simple.parking.entity.ParkingRefundType;
import com.freepark.cloud.simple.parking.service.ParkingOrderRefundService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 停车订单退款记录（停车管理 - 退款记录）。
 */
@RestController
@RequestMapping("/api/parking-order-refunds")
public class ParkingOrderRefundController {

    private final ParkingOrderRefundService parkingOrderRefundService;

    public ParkingOrderRefundController(ParkingOrderRefundService parkingOrderRefundService) {
        this.parkingOrderRefundService = parkingOrderRefundService;
    }

    @GetMapping
    public ApiResult<PageResult<ParkingOrderRefundView>> list(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long lotId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ParkingRefundType refundType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResult.ok(parkingOrderRefundService.listRefunds(
                orderId, lotId, keyword, refundType, startDate, endDate, page, size));
    }
}

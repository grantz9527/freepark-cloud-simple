package com.freepark.cloud.simple.parking.web;

import com.freepark.cloud.simple.parking.dto.EdgeArrearsQuoteRequest;
import com.freepark.cloud.simple.parking.dto.EdgeArrearsQuoteResponse;
import com.freepark.cloud.simple.parking.service.ParkingSessionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 边缘节点算费接口：供 local_server 节点配置的「算费请求接口」调用。
 * 该路径对边缘节点开放（无需 JWT），与云端其它管理接口隔离。
 */
@RestController
@RequestMapping("/api/edge")
public class EdgeArrearsQuoteController {

    private final ParkingSessionService parkingSessionService;

    public EdgeArrearsQuoteController(ParkingSessionService parkingSessionService) {
        this.parkingSessionService = parkingSessionService;
    }

    /** 算费请求：返回车牌当前的欠费金额（顶层 {@code amount}，单位元）。 */
    @PostMapping("/arrears-quote")
    public EdgeArrearsQuoteResponse quote(@RequestBody EdgeArrearsQuoteRequest request) {
        return new EdgeArrearsQuoteResponse(parkingSessionService.quoteArrearsAmount(
                request.lotCode(), request.plateNumber()));
    }
}

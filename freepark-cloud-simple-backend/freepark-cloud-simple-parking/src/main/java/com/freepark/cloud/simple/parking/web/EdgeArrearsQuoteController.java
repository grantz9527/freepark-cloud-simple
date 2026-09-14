package com.freepark.cloud.simple.parking.web;

import com.freepark.cloud.simple.parking.dto.EdgeArrearsQuoteRequest;
import com.freepark.cloud.simple.parking.dto.EdgeArrearsQuoteResponse;
import com.freepark.cloud.simple.parking.service.EdgeLaneWaitService;
import com.freepark.cloud.simple.parking.service.ParkingSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 边缘节点算费接口：供 local_server 节点配置的「算费请求接口」调用。
 * 该路径对边缘节点开放（无需 JWT），与云端其它管理接口隔离。
 *
 * <p>欠费拦截不会上报离场流水：识别路径带 {@code laneCode} 时，本接口在返回金额的同时
 * 记下或清空该通道的缴费离场等待，供后续缴费开闸使用。</p>
 */
@RestController
@RequestMapping("/api/edge")
public class EdgeArrearsQuoteController {

    private static final Logger log = LoggerFactory.getLogger(EdgeArrearsQuoteController.class);

    private final ParkingSessionService parkingSessionService;
    private final EdgeLaneWaitService laneWait;

    public EdgeArrearsQuoteController(ParkingSessionService parkingSessionService,
                                      EdgeLaneWaitService laneWait) {
        this.parkingSessionService = parkingSessionService;
        this.laneWait = laneWait;
    }

    /** 算费请求：返回车牌当前的欠费金额（顶层 {@code amount}，单位元）。 */
    @PostMapping("/arrears-quote")
    public EdgeArrearsQuoteResponse quote(@RequestBody EdgeArrearsQuoteRequest request) {
        BigDecimal amount = parkingSessionService.quoteArrearsAmount(
                request.lotCode(), request.plateNumber());
        try {
            laneWait.rememberFromQuote(request, amount);
        } catch (RuntimeException ex) {
            log.warn("算费后登记通道等待失败 lane={} plate={}：{}",
                    request == null ? null : request.laneCode(),
                    request == null ? null : request.plateNumber(),
                    ex.getMessage());
        }
        return new EdgeArrearsQuoteResponse(amount);
    }
}

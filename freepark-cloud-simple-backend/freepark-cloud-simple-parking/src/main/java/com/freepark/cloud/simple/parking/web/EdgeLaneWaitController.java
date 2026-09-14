package com.freepark.cloud.simple.parking.web;

import com.freepark.cloud.simple.parking.dto.EdgeLaneWaitRequest;
import com.freepark.cloud.simple.parking.dto.EdgeLaneWaitView;
import com.freepark.cloud.simple.parking.service.EdgeLaneWaitService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 边缘上报通道最新识别等待：欠费拦截后记下车牌，新识别覆盖或 CLEAR。
 * 识别算费已兼记等待，本接口仅兼容旧边缘；路径对边缘开放（无需 JWT）。
 */
@RestController
@RequestMapping("/api/edge")
public class EdgeLaneWaitController {

    private final EdgeLaneWaitService waitService;

    public EdgeLaneWaitController(EdgeLaneWaitService waitService) {
        this.waitService = waitService;
    }

    @PostMapping("/lane-wait")
    public EdgeLaneWaitView report(@RequestBody EdgeLaneWaitRequest request) {
        return waitService.report(request);
    }
}

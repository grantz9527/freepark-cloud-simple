package com.freepark.cloud.simple.settings.web;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.settings.dto.EdgeMqttConfigView;
import com.freepark.cloud.simple.settings.dto.UpdateEdgeMqttConfigRequest;
import com.freepark.cloud.simple.settings.service.EdgeMqttConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 边缘计算配置：云端订阅停车系统上报数据、并向其它边缘计算服务发布配置的
 * MQTT 参数（写操作仅超级管理员）。
 */
@RestController
@RequestMapping("/api/system/edge-mqtt")
public class EdgeMqttConfigController {

    private final EdgeMqttConfigService configService;

    public EdgeMqttConfigController(EdgeMqttConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    public ApiResult<EdgeMqttConfigView> get() {
        return ApiResult.ok(configService.getConfig());
    }

    @PutMapping
    public ApiResult<EdgeMqttConfigView> update(@RequestBody UpdateEdgeMqttConfigRequest request) {
        return ApiResult.ok(configService.updateConfig(request));
    }

    /**
     * 使用请求携带的连接参数测试到远端 Broker 的连通性（不持久化）。
     */
    @PostMapping("/test")
    public ApiResult<Void> test(@RequestBody UpdateEdgeMqttConfigRequest request) {
        configService.testConnection(request);
        return ApiResult.ok();
    }
}

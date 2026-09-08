package com.freepark.cloud.simple.settings.web;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.common.i18n.BizException;
import com.freepark.cloud.simple.common.i18n.MessageKeys;
import com.freepark.cloud.simple.settings.dto.EdgeMqttConfigView;
import com.freepark.cloud.simple.settings.dto.UpdateEdgeMqttConfigRequest;
import com.freepark.cloud.simple.settings.runtime.EdgeConfigSyncDispatcher;
import com.freepark.cloud.simple.settings.service.EdgeMqttConfigService;
import com.freepark.cloud.simple.user.entity.UserAccount;
import com.freepark.cloud.simple.user.service.AdminGuard;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final EdgeConfigSyncDispatcher syncDispatcher;
    private final AdminGuard adminGuard;

    public EdgeMqttConfigController(EdgeMqttConfigService configService,
            EdgeConfigSyncDispatcher syncDispatcher, AdminGuard adminGuard) {
        this.configService = configService;
        this.syncDispatcher = syncDispatcher;
        this.adminGuard = adminGuard;
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

    /**
     * 手动触发一次边缘配置全量同步（仅超级管理员）：跳过周期节流，对全部目标
     * 节点按车场逐域下发 v3 快照帧。
     */
    @PostMapping("/config-sync/full")
    public ApiResult<EdgeConfigSyncDispatcher.DispatchSummary> triggerFullSync() {
        requireSuperAdmin();
        return ApiResult.ok(syncDispatcher.fullSyncNow());
    }

    /**
     * 对单个边缘节点即时补发一次全量快照（仅超级管理员；该节点必须仍是同步目标，
     * 否则返回空结果）。
     */
    @PostMapping("/config-sync/nodes/{nodeCode}")
    public ApiResult<EdgeConfigSyncDispatcher.DispatchSummary> triggerNodeSync(
            @PathVariable String nodeCode) {
        requireSuperAdmin();
        return ApiResult.ok(syncDispatcher.syncNodeNow(nodeCode));
    }

    private void requireSuperAdmin() {
        UserAccount operator = adminGuard.requireEnabledAdmin();
        if (!UserAccount.ROLE_SUPER_ADMIN.equals(operator.getRole())) {
            throw new BizException(403, MessageKeys.AUTH_FORBIDDEN);
        }
    }
}

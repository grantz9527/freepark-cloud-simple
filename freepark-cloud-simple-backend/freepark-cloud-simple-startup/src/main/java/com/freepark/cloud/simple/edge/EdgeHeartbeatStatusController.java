package com.freepark.cloud.simple.edge;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.user.service.AdminGuard;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 边缘监控-车场上行心跳在线状态查询（只读，启用中的管理员可访问；
 * 页面入口与边缘计算配置同属系统管理菜单，按菜单角色限制为超管）。
 */
@RestController
@RequestMapping("/api/system/edge-heartbeat")
public class EdgeHeartbeatStatusController {

    private final AdminGuard adminGuard;
    private final LotEdgeHeartbeatListener heartbeatListener;

    public EdgeHeartbeatStatusController(AdminGuard adminGuard,
            LotEdgeHeartbeatListener heartbeatListener) {
        this.adminGuard = adminGuard;
        this.heartbeatListener = heartbeatListener;
    }

    @GetMapping("/status")
    public ApiResult<EdgeHeartbeatStatusView> status() {
        adminGuard.requireEnabledAdmin();
        return ApiResult.ok(heartbeatListener.status());
    }
}

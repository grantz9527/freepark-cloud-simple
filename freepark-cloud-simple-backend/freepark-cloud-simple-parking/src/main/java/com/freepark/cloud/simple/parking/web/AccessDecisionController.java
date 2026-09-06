package com.freepark.cloud.simple.parking.web;

import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.parking.dto.AccessDecisionRequest;
import com.freepark.cloud.simple.parking.dto.AccessDecisionView;
import com.freepark.cloud.simple.parking.service.AccessDecisionService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通行判定：按车场配置与名单数据模拟判定放行/拦截。
 */
@RestController
@RequestMapping("/api/lots/{lotId}/access-decision")
public class AccessDecisionController {

    private final AccessDecisionService accessDecisionService;

    public AccessDecisionController(AccessDecisionService accessDecisionService) {
        this.accessDecisionService = accessDecisionService;
    }

    @PostMapping
    public ApiResult<AccessDecisionView> decide(@PathVariable Long lotId,
                                                @RequestBody AccessDecisionRequest request) {
        return ApiResult.ok(accessDecisionService.decide(lotId, request));
    }
}

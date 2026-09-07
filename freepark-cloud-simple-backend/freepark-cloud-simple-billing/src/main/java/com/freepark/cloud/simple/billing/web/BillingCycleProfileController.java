package com.freepark.cloud.simple.billing.web;

import com.freepark.cloud.simple.billing.dto.BillingCycleProfileRequest;
import com.freepark.cloud.simple.billing.dto.BillingCycleProfileView;
import com.freepark.cloud.simple.billing.service.BillingCycleProfileService;
import com.freepark.cloud.simple.common.ApiResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 计费周期方案管理（全局共享方案，供各车场的 24 小时制规则选用）。
 */
@RestController
@RequestMapping("/api/billing/cycle-profiles")
public class BillingCycleProfileController {

    private final BillingCycleProfileService profileService;

    public BillingCycleProfileController(BillingCycleProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ApiResult<List<BillingCycleProfileView>> list() {
        return ApiResult.ok(profileService.list());
    }

    @PostMapping
    public ApiResult<BillingCycleProfileView> create(@RequestBody BillingCycleProfileRequest request) {
        return ApiResult.ok(profileService.create(request));
    }

    @PutMapping("/{profileId}")
    public ApiResult<BillingCycleProfileView> update(@PathVariable Long profileId,
                                                     @RequestBody BillingCycleProfileRequest request) {
        return ApiResult.ok(profileService.update(profileId, request));
    }

    @DeleteMapping("/{profileId}")
    public ApiResult<Void> delete(@PathVariable Long profileId) {
        profileService.delete(profileId);
        return ApiResult.ok();
    }
}

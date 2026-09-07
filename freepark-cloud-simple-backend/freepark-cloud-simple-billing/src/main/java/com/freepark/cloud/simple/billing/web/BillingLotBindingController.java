package com.freepark.cloud.simple.billing.web;

import com.freepark.cloud.simple.billing.dto.BillingLotBindingRequest;
import com.freepark.cloud.simple.billing.dto.BillingLotBindingView;
import com.freepark.cloud.simple.billing.service.BillingLotBindingService;
import com.freepark.cloud.simple.common.ApiResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 车场计费配置管理：把每日制/24 小时制规则模板按「车场 + 车牌颜色 + 生效起止日期」手动绑定，
 * 供云端算费引擎按车场取用。列表按车场过滤时可省略 lotId 查看全量。
 */
@RestController
@RequestMapping("/api/billing/lot-bindings")
public class BillingLotBindingController {

    private final BillingLotBindingService bindingService;

    public BillingLotBindingController(BillingLotBindingService bindingService) {
        this.bindingService = bindingService;
    }

    @GetMapping
    public ApiResult<List<BillingLotBindingView>> list(
            @RequestParam(required = false) Long lotId) {
        return ApiResult.ok(bindingService.list(lotId));
    }

    @PostMapping
    public ApiResult<BillingLotBindingView> create(@RequestBody BillingLotBindingRequest request) {
        return ApiResult.ok(bindingService.create(request));
    }

    @PutMapping("/{bindingId}")
    public ApiResult<BillingLotBindingView> update(@PathVariable Long bindingId,
                                                   @RequestBody BillingLotBindingRequest request) {
        return ApiResult.ok(bindingService.update(bindingId, request));
    }

    @DeleteMapping("/{bindingId}")
    public ApiResult<Void> delete(@PathVariable Long bindingId) {
        bindingService.delete(bindingId);
        return ApiResult.ok();
    }
}

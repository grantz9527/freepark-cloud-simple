package com.freepark.cloud.simple.billing.web;

import com.freepark.cloud.simple.billing.dto.BillingDateRequest;
import com.freepark.cloud.simple.billing.dto.BillingSpecialDateView;
import com.freepark.cloud.simple.billing.service.BillingSpecialDateService;
import com.freepark.cloud.simple.common.ApiResult;
import com.freepark.cloud.simple.common.web.PageResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 计费特殊日期（节假日区间 / 补班日期）管理，全局生效。
 */
@RestController
@RequestMapping("/api/billing/date-rules")
public class BillingDateRuleController {

    private final BillingSpecialDateService dateService;

    public BillingDateRuleController(BillingSpecialDateService dateService) {
        this.dateService = dateService;
    }

    @GetMapping
    public ApiResult<PageResult<BillingSpecialDateView>> list(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResult.ok(dateService.list(type, page, size));
    }

    @PostMapping
    public ApiResult<BillingSpecialDateView> create(@RequestBody BillingDateRequest request) {
        return ApiResult.ok(dateService.create(request));
    }

    @PutMapping("/{dateId}")
    public ApiResult<BillingSpecialDateView> update(@PathVariable Long dateId,
                                                    @RequestBody BillingDateRequest request) {
        return ApiResult.ok(dateService.update(dateId, request));
    }

    @DeleteMapping("/{dateId}")
    public ApiResult<Void> delete(@PathVariable Long dateId) {
        dateService.delete(dateId);
        return ApiResult.ok();
    }
}

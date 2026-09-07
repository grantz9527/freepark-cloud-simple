package com.freepark.cloud.simple.billing.web;

import com.freepark.cloud.simple.billing.dto.BillingDailyRuleRequest;
import com.freepark.cloud.simple.billing.dto.BillingDailyRuleView;
import com.freepark.cloud.simple.billing.dto.BillingSimulateRequest;
import com.freepark.cloud.simple.billing.dto.BillingSimulateResult;
import com.freepark.cloud.simple.billing.service.BillingDailyRuleService;
import com.freepark.cloud.simple.billing.service.BillingSimulateService;
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
 * 每日制计费规则（全局模板）管理。模板本身不绑定车场/车牌颜色，
 * 车场侧通过「计费配置」引用并指定颜色与生效时段（见 BillingLotBindingController）。
 * 每条模板带「周计划」：某天未配置时默认全天收费、配置时段则仅时段收费、也可整体标记为当天免费。
 */
@RestController
@RequestMapping("/api/billing/daily-rules")
public class BillingDailyRuleController {

    private final BillingDailyRuleService ruleService;
    private final BillingSimulateService simulateService;

    public BillingDailyRuleController(BillingDailyRuleService ruleService,
                                      BillingSimulateService simulateService) {
        this.ruleService = ruleService;
        this.simulateService = simulateService;
    }

    @GetMapping
    public ApiResult<List<BillingDailyRuleView>> list() {
        return ApiResult.ok(ruleService.list());
    }

    @PostMapping
    public ApiResult<BillingDailyRuleView> create(@RequestBody BillingDailyRuleRequest request) {
        return ApiResult.ok(ruleService.create(request));
    }

    @PutMapping("/{ruleId}")
    public ApiResult<BillingDailyRuleView> update(@PathVariable Long ruleId,
                                                  @RequestBody BillingDailyRuleRequest request) {
        return ApiResult.ok(ruleService.update(ruleId, request));
    }

    @DeleteMapping("/{ruleId}")
    public ApiResult<Void> delete(@PathVariable Long ruleId) {
        ruleService.delete(ruleId);
        return ApiResult.ok();
    }

    /** 按给定「入场 → 出场」时间段模拟计算该每日制规则应收金额。 */
    @PostMapping("/{ruleId}/simulate-charge")
    public ApiResult<BillingSimulateResult> simulate(@PathVariable Long ruleId,
                                                     @RequestBody BillingSimulateRequest request) {
        return ApiResult.ok(simulateService.simulateDaily(ruleId, request));
    }
}

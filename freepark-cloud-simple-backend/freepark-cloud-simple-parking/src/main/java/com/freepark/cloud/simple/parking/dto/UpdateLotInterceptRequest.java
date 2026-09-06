package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.InterceptRuleType;

import java.util.List;

/**
 * 更新车场入口/出口拦截规则请求。
 */
public record UpdateLotInterceptRequest(List<InterceptRuleType> entryRules, List<InterceptRuleType> exitRules) {
}

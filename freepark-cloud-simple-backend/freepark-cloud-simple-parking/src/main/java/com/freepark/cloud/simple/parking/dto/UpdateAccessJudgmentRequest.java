package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.AccessJudgmentRuleType;

import java.util.List;

/**
 * 更新通行判定规则顺序请求。
 */
public record UpdateAccessJudgmentRequest(List<AccessJudgmentRuleType> ruleOrder) {
}

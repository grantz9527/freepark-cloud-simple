package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.AccessJudgmentRuleType;
import com.freepark.cloud.simple.parking.entity.ParkingLot;

import java.util.EnumSet;
import java.util.List;

/**
 * 通行判定规则顺序视图。
 */
public record AccessJudgmentView(List<AccessJudgmentRuleType> ruleOrder) {

    public static AccessJudgmentView from(ParkingLot lot) {
        return new AccessJudgmentView(lot.effectiveJudgmentOrder());
    }

    public static List<AccessJudgmentRuleType> defaultOrder() {
        return AccessJudgmentRuleType.defaultOrder();
    }

    /**
     * 校验规则顺序：必须恰好包含全部规则且不重复。
     */
    public static void validateOrder(List<AccessJudgmentRuleType> ruleOrder) {
        if (ruleOrder == null || ruleOrder.size() != AccessJudgmentRuleType.values().length) {
            throw new IllegalArgumentException("invalid access judgment order size");
        }
        if (ruleOrder.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("invalid access judgment order values");
        }
        if (!EnumSet.copyOf(ruleOrder).equals(EnumSet.allOf(AccessJudgmentRuleType.class))) {
            throw new IllegalArgumentException("invalid access judgment order values");
        }
    }
}

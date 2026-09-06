package com.freepark.cloud.simple.parking.entity;

import java.util.List;

/**
 * 通行判定规则类型。
 */
public enum AccessJudgmentRuleType {

    /** 车牌号段放行（放行名单） */
    PATTERN_ALLOWLIST,

    /** 黑名单拦截 */
    BLACKLIST,

    /** 白名单放行 */
    WHITELIST;

    /** 默认判定顺序：黑名单 → 白名单 → 号段放行 */
    public static List<AccessJudgmentRuleType> defaultOrder() {
        return List.of(BLACKLIST, WHITELIST, PATTERN_ALLOWLIST);
    }
}

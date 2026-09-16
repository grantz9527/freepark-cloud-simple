package com.freepark.cloud.simple.parking.entity;

/**
 * 拦截规则类型。
 */
public enum InterceptRuleType {

    /** 欠费拦截 */
    ARREARS,

    /** 黑名单拦截 */
    BLACKLIST,

    /** 满位拦截（仅入口：在场车辆数达到车位总数时禁止入场） */
    FULL
}

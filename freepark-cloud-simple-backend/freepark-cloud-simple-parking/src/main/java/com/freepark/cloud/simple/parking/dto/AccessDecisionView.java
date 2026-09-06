package com.freepark.cloud.simple.parking.dto;

/**
 * 通行判定结果。
 */
public record AccessDecisionView(Result result, String remark) {

    public enum Result {
        ALLOWED,
        INTERCEPTED
    }

    public static AccessDecisionView allowed(String remark) {
        return new AccessDecisionView(Result.ALLOWED, remark);
    }

    public static AccessDecisionView intercepted(String remark) {
        return new AccessDecisionView(Result.INTERCEPTED, remark);
    }
}

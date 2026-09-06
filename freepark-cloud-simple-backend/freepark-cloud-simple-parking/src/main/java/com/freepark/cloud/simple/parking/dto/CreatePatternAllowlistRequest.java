package com.freepark.cloud.simple.parking.dto;

/**
 * 新建放行名单（号段规则）请求。
 */
public record CreatePatternAllowlistRequest(
        String name,
        String pattern,
        String remark,
        Boolean enabled) {
}

package com.freepark.cloud.simple.parking.dto;

/**
 * 更新放行名单（号段规则）请求。
 */
public record UpdatePatternAllowlistRequest(
        String name,
        String pattern,
        String remark,
        Boolean enabled) {
}

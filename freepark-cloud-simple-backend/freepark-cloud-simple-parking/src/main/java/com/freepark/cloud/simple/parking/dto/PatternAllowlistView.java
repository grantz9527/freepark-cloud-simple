package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.PatternAllowlist;

import java.time.LocalDateTime;

/**
 * 放行名单（号段规则）视图。
 */
public record PatternAllowlistView(
        Long id,
        Long lotId,
        String lotName,
        String name,
        String pattern,
        String remark,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static PatternAllowlistView from(PatternAllowlist entry) {
        return new PatternAllowlistView(
                entry.getId(),
                entry.getLot().getId(),
                entry.getLot().getName(),
                entry.getName(),
                entry.getPattern(),
                entry.getRemark(),
                entry.isEnabled(),
                entry.getCreatedAt(),
                entry.getUpdatedAt());
    }
}

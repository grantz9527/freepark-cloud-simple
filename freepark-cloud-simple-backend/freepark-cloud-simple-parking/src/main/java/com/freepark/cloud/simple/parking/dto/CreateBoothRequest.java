package com.freepark.cloud.simple.parking.dto;

import java.util.List;

/**
 * 新建岗亭请求。
 */
public record CreateBoothRequest(
        String name,
        String code,
        String location,
        Boolean enabled,
        List<Long> laneIds) {
}

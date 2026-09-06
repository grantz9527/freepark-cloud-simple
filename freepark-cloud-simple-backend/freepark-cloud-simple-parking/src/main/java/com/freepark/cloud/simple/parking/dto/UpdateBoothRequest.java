package com.freepark.cloud.simple.parking.dto;

import java.util.List;

/**
 * 更新岗亭请求。
 */
public record UpdateBoothRequest(
        String name,
        String code,
        String location,
        Boolean enabled,
        List<Long> laneIds) {
}

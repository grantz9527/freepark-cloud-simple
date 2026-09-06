package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.LaneType;

/**
 * 新建通道请求。
 */
public record CreateLaneRequest(
        String name,
        String code,
        LaneType laneType,
        Long lotId,
        Long linkedLotId,
        Boolean enabled) {
}

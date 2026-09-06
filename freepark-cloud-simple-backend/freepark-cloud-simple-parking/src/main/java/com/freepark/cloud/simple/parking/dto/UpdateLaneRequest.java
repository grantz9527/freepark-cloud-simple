package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.LaneType;

/**
 * 更新通道请求（编码创建后不可改）。
 */
public record UpdateLaneRequest(
        String name,
        LaneType laneType,
        Long lotId,
        Long linkedLotId,
        Boolean enabled) {
}

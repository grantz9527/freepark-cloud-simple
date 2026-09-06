package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.PlateColor;

import java.util.List;

/**
 * 通行判定请求：根据车场判定顺序与名单数据决定是否放行。
 */
public record AccessDecisionRequest(
        Long laneId,
        String plateNumber,
        PlateColor plateColor,
        String direction,
        List<PlateColor> interceptColors,
        Boolean hasOpenSession) {
}

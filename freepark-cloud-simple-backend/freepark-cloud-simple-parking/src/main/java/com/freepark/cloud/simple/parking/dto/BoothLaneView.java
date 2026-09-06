package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.ParkingLane;

/**
 * 岗亭绑定的通道简视图。
 */
public record BoothLaneView(Long id, String name, String code, String laneType) {

    public static BoothLaneView from(ParkingLane lane) {
        return new BoothLaneView(
                lane.getId(),
                lane.getName(),
                lane.getCode(),
                lane.getLaneType().name());
    }
}

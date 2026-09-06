package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.ParkingLane;
import com.freepark.cloud.simple.parking.entity.ParkingLot;

import java.time.LocalDateTime;

/**
 * 通道视图。
 */
public record LaneView(
        Long id,
        Long lotId,
        String lotName,
        String lotCode,
        Long linkedLotId,
        String linkedLotName,
        String linkedLotCode,
        String name,
        String code,
        String laneType,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static LaneView from(ParkingLane lane) {
        ParkingLot lot = lane.getLot();
        ParkingLot linkedLot = lane.getLinkedLot();
        return new LaneView(
                lane.getId(),
                lot.getId(),
                lot.getName(),
                lot.getCode(),
                linkedLot == null ? null : linkedLot.getId(),
                linkedLot == null ? null : linkedLot.getName(),
                linkedLot == null ? null : linkedLot.getCode(),
                lane.getName(),
                lane.getCode(),
                lane.getLaneType().name(),
                lane.isEnabled(),
                lane.getCreatedAt(),
                lane.getUpdatedAt());
    }
}

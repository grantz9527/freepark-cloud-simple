package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.ParkingSpace;

import java.time.LocalDateTime;

/**
 * 车位视图。
 */
public record SpaceView(
        Long id,
        Long lotId,
        Long areaId,
        String areaName,
        String locationName,
        String code,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static SpaceView from(ParkingSpace space) {
        return new SpaceView(
                space.getId(),
                space.getLot().getId(),
                space.getArea().getId(),
                space.getArea().getName(),
                space.getArea().getLocation().getName(),
                space.getCode(),
                space.isEnabled(),
                space.getCreatedAt(),
                space.getUpdatedAt());
    }
}

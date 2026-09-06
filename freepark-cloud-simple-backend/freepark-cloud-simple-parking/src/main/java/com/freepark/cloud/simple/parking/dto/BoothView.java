package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.ParkingBooth;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 岗亭视图。
 */
public record BoothView(
        Long id,
        Long lotId,
        String lotName,
        String name,
        String code,
        String location,
        boolean enabled,
        List<BoothLaneView> lanes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static BoothView from(ParkingBooth booth) {
        return new BoothView(
                booth.getId(),
                booth.getLot().getId(),
                booth.getLot().getName(),
                booth.getName(),
                booth.getCode(),
                booth.getLocation(),
                booth.isEnabled(),
                booth.getLanes().stream().map(BoothLaneView::from).toList(),
                booth.getCreatedAt(),
                booth.getUpdatedAt());
    }
}

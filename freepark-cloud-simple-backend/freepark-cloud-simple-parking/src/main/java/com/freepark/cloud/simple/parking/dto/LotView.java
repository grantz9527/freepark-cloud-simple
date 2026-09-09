package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.ParkingLot;

import java.time.LocalDateTime;

/**
 * 停车场视图。
 */
public record LotView(
        Long id,
        String name,
        String code,
        String lotType,
        String address,
        int totalSpaces,
        boolean enabled,
        String mapData,
        String arrearsScope,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static LotView from(ParkingLot lot) {
        return new LotView(
                lot.getId(),
                lot.getName(),
                lot.getCode(),
                lot.getLotType().name(),
                lot.getAddress(),
                lot.getTotalSpaces(),
                lot.isEnabled(),
                lot.getMapData(),
                lot.getArrearsScope().name(),
                lot.getCreatedAt(),
                lot.getUpdatedAt());
    }
}

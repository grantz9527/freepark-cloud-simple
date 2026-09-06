package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.WhitelistVehicle;

import java.time.LocalDateTime;

/**
 * 白名单车辆视图。
 */
public record WhitelistVehicleView(
        Long id,
        Long lotId,
        String lotName,
        String plateNumber,
        String plateColor,
        String ownerName,
        String type,
        String phone,
        String department,
        String remark,
        LocalDateTime startTime,
        LocalDateTime endTime,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static WhitelistVehicleView from(WhitelistVehicle vehicle) {
        return new WhitelistVehicleView(
                vehicle.getId(),
                vehicle.getLot().getId(),
                vehicle.getLot().getName(),
                vehicle.getPlateNumber(),
                vehicle.getPlateColor().name(),
                vehicle.getOwnerName(),
                vehicle.getType().name(),
                vehicle.getPhone(),
                vehicle.getDepartment(),
                vehicle.getRemark(),
                vehicle.getStartTime(),
                vehicle.getEndTime(),
                vehicle.isEnabled(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt());
    }
}

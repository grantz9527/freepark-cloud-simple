package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.BlacklistVehicle;

import java.time.LocalDateTime;

/**
 * 黑名单车辆视图。
 */
public record BlacklistVehicleView(
        Long id,
        Long lotId,
        String lotName,
        String plateNumber,
        String plateColor,
        String ownerName,
        String phone,
        String department,
        String remark,
        LocalDateTime startTime,
        LocalDateTime endTime,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static BlacklistVehicleView from(BlacklistVehicle vehicle) {
        return new BlacklistVehicleView(
                vehicle.getId(),
                vehicle.getLot().getId(),
                vehicle.getLot().getName(),
                vehicle.getPlateNumber(),
                vehicle.getPlateColor().name(),
                vehicle.getOwnerName(),
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

package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.InternalVehicle;

import java.time.LocalDateTime;

/**
 * 内部车辆视图。
 */
public record InternalVehicleView(
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
        String batchId,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static InternalVehicleView from(InternalVehicle vehicle) {
        return new InternalVehicleView(
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
                vehicle.getBatchId(),
                vehicle.isEnabled(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt());
    }
}

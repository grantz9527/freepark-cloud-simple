package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.DiscountVehicle;

import java.time.LocalDateTime;

/**
 * 优惠车辆视图。
 *
 * @param freeMinutes 每次入场的免费时长（分钟）：该车每次入场自入场时刻起免费停 N 分钟，超出部分照常计费
 */
public record DiscountVehicleView(
        Long id,
        Long lotId,
        String lotName,
        String plateNumber,
        String plateColor,
        Integer freeMinutes,
        boolean enabled,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static DiscountVehicleView from(DiscountVehicle vehicle) {
        return new DiscountVehicleView(
                vehicle.getId(),
                vehicle.getLot().getId(),
                vehicle.getLot().getName(),
                vehicle.getPlateNumber(),
                vehicle.getPlateColor().name(),
                vehicle.getFreeMinutes(),
                vehicle.isEnabled(),
                vehicle.getRemark(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt());
    }
}

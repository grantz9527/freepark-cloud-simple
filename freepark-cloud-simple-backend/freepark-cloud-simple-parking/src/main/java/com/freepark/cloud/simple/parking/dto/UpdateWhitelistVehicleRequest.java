package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.PlateColor;
import com.freepark.cloud.simple.parking.entity.VehicleType;

import java.time.LocalDateTime;

/**
 * 更新白名单车辆（停车卡）请求。
 */
public record UpdateWhitelistVehicleRequest(
        String plateNumber,
        PlateColor plateColor,
        String ownerName,
        VehicleType type,
        String phone,
        String department,
        String remark,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Boolean enabled) {
}

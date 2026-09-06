package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.PlateColor;

import java.time.LocalDateTime;

/**
 * 更新黑名单车辆请求。
 */
public record UpdateBlacklistVehicleRequest(
        String plateNumber,
        PlateColor plateColor,
        String ownerName,
        String phone,
        String department,
        String remark,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Boolean enabled) {
}

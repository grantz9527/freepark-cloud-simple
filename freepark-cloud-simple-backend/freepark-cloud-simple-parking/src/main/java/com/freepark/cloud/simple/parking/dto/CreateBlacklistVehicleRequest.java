package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.PlateColor;

import java.time.LocalDateTime;

/**
 * 新建黑名单车辆请求。
 */
public record CreateBlacklistVehicleRequest(
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

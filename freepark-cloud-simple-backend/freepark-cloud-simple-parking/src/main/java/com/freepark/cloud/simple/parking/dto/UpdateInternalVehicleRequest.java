package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.PlateColor;
import com.freepark.cloud.simple.parking.entity.VehicleType;

/**
 * 更新内部车辆请求。
 */
public record UpdateInternalVehicleRequest(
        String plateNumber,
        PlateColor plateColor,
        String ownerName,
        VehicleType type,
        String phone,
        String department,
        String remark,
        Boolean enabled) {
}

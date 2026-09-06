package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.PlateColor;
import com.freepark.cloud.simple.parking.entity.VehicleType;

/**
 * 新建内部车辆请求。
 */
public record CreateInternalVehicleRequest(
        String plateNumber,
        PlateColor plateColor,
        String ownerName,
        VehicleType type,
        String phone,
        String department,
        String remark,
        Boolean enabled) {
}

package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.PlateColor;

/**
 * 更新优惠车辆请求（指定车辆每次入场免费时长）。
 */
public record UpdateDiscountVehicleRequest(
        String plateNumber,
        PlateColor plateColor,
        Integer freeMinutes,
        String remark,
        Boolean enabled) {
}

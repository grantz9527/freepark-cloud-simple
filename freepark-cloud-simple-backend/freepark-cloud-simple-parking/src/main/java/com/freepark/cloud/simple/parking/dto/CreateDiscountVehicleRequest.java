package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.PlateColor;

/**
 * 新增优惠车辆请求（指定车辆每次入场免费时长）。
 */
public record CreateDiscountVehicleRequest(
        String plateNumber,
        PlateColor plateColor,
        Integer freeMinutes,
        String remark) {
}

package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.LotArrearsScope;
import com.freepark.cloud.simple.parking.entity.LotType;

/**
 * 更新停车场请求。
 */
public record UpdateLotRequest(
        String name,
        LotType lotType,
        String address,
        Integer totalSpaces,
        Boolean enabled,
        String mapData,
        LotArrearsScope arrearsScope) {
}

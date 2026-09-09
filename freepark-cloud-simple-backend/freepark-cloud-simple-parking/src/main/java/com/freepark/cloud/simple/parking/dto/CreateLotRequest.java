package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.LotArrearsScope;
import com.freepark.cloud.simple.parking.entity.LotType;

/**
 * 新建停车场请求。
 */
public record CreateLotRequest(
        String name,
        String code,
        LotType lotType,
        String address,
        Integer totalSpaces,
        Boolean enabled,
        LotArrearsScope arrearsScope) {
}

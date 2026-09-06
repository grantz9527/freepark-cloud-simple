package com.freepark.cloud.simple.parking.dto;

/**
 * 更新车位请求。
 */
public record UpdateSpaceRequest(Long areaId, String code, Boolean enabled) {
}

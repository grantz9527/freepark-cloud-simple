package com.freepark.cloud.simple.parking.dto;

/**
 * 新建车位请求。
 */
public record CreateSpaceRequest(Long areaId, String code, Boolean enabled) {
}

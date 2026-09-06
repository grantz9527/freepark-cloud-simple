package com.freepark.cloud.simple.parking.dto;

/**
 * 新建停车区域请求。
 */
public record CreateAreaRequest(Long locationId, String name) {
}

package com.freepark.cloud.simple.parking.dto;

/**
 * 创建停车订单请求。
 *
 * @param sessionId 关联的停车流水 ID
 */
public record CreateParkingOrderRequest(Long sessionId) {
}

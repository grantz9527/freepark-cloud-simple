package com.freepark.cloud.simple.parking.event;

/**
 * 云端停车流水被改写（事务提交后经 MQTT 下发到边缘节点）。
 *
 * @param sessionId 云端流水主键
 */
public record ParkingSessionChangedEvent(Long sessionId) {
}

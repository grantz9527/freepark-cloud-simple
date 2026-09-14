package com.freepark.cloud.simple.parking.dto;

/**
 * 边缘上报通道最新识别等待状态（欠费拦截 / 清除）。
 *
 * <p>{@code reason=ARREARS}：该通道因欠费拦截，记下车牌；后续新识别必须再报一次（覆盖或 CLEAR）。
 * {@code reason=CLEAR}：新识别未欠费拦截，或车辆已离开，清空等待。</p>
 */
public record EdgeLaneWaitRequest(
        String edgeCode,
        String laneCode,
        String lotCode,
        String plateNumber,
        String plateColor,
        String reason,
        String recognizedAt,
        String recognitionId) {
}

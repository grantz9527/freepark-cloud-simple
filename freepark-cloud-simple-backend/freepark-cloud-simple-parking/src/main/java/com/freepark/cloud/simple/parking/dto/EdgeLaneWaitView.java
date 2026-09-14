package com.freepark.cloud.simple.parking.dto;

/**
 * 通道等待状态上报结果。
 *
 * @param waiting true 表示通道当前记着欠费拦截等待
 */
public record EdgeLaneWaitView(boolean waiting, String laneCode, String plateNumber) {

    public static EdgeLaneWaitView waiting(String laneCode, String plateNumber) {
        return new EdgeLaneWaitView(true, laneCode, plateNumber);
    }

    public static EdgeLaneWaitView cleared(String laneCode) {
        return new EdgeLaneWaitView(false, laneCode, null);
    }
}

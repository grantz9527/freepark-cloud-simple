package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.PlateColor;

import java.time.LocalDateTime;

/**
 * 手动新增停车流水（入场）请求。
 *
 * @param lotId        车场 ID（必填，用于快照车场名称）
 * @param plateNumber  车牌号（必填）
 * @param plateColor   车牌颜色（可选，默认蓝牌）
 * @param entryTime    入场时间（可选，默认当前时间）
 * @param entryLaneId  入场通道 ID（可选）
 * @param entryLaneName 入场通道名称（可选，优先由前端传入）
 * @param entryImage   入场抓拍图片（可选）
 */
public record CreateParkingSessionRequest(
        Long lotId,
        String plateNumber,
        PlateColor plateColor,
        LocalDateTime entryTime,
        Long entryLaneId,
        String entryLaneName,
        String entryImage) {
}

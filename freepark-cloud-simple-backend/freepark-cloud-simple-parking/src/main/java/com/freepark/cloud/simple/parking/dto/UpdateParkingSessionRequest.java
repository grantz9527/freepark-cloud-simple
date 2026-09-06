package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.PlateColor;

import java.time.LocalDateTime;

/**
 * 编辑停车流水请求：可修正入场信息；在场流水提供出场信息后自动关场（CLOSED）。
 *
 * @param plateNumber    车牌号（可选，留空则不修改）
 * @param plateColor     车牌颜色（可选）
 * @param entryTime      入场时间（可选）
 * @param entryLaneId    入场通道 ID（可选）
 * @param entryLaneName  入场通道名称（可选）
 * @param entryImage     入场抓拍图片（可选）
 * @param exitTime       出场时间（在场流水提供该值则自动关场）
 * @param exitLaneId     出场通道 ID（可选）
 * @param exitLaneName   出场通道名称（可选）
 * @param exitImage      出场抓拍图片（可选）
 */
public record UpdateParkingSessionRequest(
        String plateNumber,
        PlateColor plateColor,
        LocalDateTime entryTime,
        Long entryLaneId,
        String entryLaneName,
        String entryImage,
        LocalDateTime exitTime,
        Long exitLaneId,
        String exitLaneName,
        String exitImage) {
}

package com.freepark.cloud.simple.parking.dto;

import com.freepark.cloud.simple.parking.entity.ParkingSession;

import java.time.LocalDateTime;

/**
 * 停车流水视图。
 */
public record ParkingSessionView(
        Long id,
        Long lotId,
        String lotName,
        String plateNumber,
        String plateColor,
        String status,
        LocalDateTime entryTime,
        Long entryLaneId,
        String entryLaneName,
        Long entryRecognitionId,
        String entryImage,
        LocalDateTime exitTime,
        Long exitLaneId,
        String exitLaneName,
        Long exitRecognitionId,
        String exitImage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ParkingSessionView from(ParkingSession session) {
        return new ParkingSessionView(
                session.getId(),
                session.getLotId(),
                session.getLotName(),
                session.getPlateNumber(),
                session.getPlateColor() == null ? null : session.getPlateColor().name(),
                session.getStatus() == null ? null : session.getStatus().name(),
                session.getEntryTime(),
                session.getEntryLaneId(),
                session.getEntryLaneName(),
                session.getEntryRecognitionId(),
                session.getEntryImage(),
                session.getExitTime(),
                session.getExitLaneId(),
                session.getExitLaneName(),
                session.getExitRecognitionId(),
                session.getExitImage(),
                session.getCreatedAt(),
                session.getUpdatedAt());
    }
}

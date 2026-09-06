package com.freepark.cloud.simple.parking.repository;

import com.freepark.cloud.simple.parking.entity.ParkingSession;
import com.freepark.cloud.simple.parking.entity.ParkingSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ParkingSessionRepository
        extends JpaRepository<ParkingSession, Long>, JpaSpecificationExecutor<ParkingSession> {

    boolean existsByLotIdAndPlateNumberIgnoreCaseAndStatus(Long lotId, String plate, ParkingSessionStatus status);

    List<ParkingSession> findAllByLotIdAndPlateNumberIgnoreCaseAndStatus(
            Long lotId, String plate, ParkingSessionStatus status);

    Optional<ParkingSession> findFirstByLotIdAndPlateNumberIgnoreCaseAndStatusOrderByEntryTimeDesc(
            Long lotId, String plate, ParkingSessionStatus status);

    List<ParkingSession> findAllByOrderByEntryTimeDesc();
}

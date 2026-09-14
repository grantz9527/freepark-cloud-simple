package com.freepark.cloud.simple.parking.repository;

import com.freepark.cloud.simple.parking.entity.InterceptRuleType;
import com.freepark.cloud.simple.parking.entity.ParkingLane;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParkingLaneRepository extends JpaRepository<ParkingLane, Long> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<ParkingLane> findAllByOrderByCreatedAtDesc();

    List<ParkingLane> findAllByLot_IdOrLinkedLot_IdOrderByCreatedAtDesc(Long lotId, Long linkedLotId);

    List<ParkingLane> findByLot_IdOrderByCreatedAtAsc(Long lotId);

    Optional<ParkingLane> findByCodeIgnoreCase(String code);

    List<ParkingLane> findByEnabledTrueAndWaitReasonAndWaitPlateNumberIgnoreCase(
            InterceptRuleType waitReason, String waitPlateNumber);
}

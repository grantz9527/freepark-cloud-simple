package com.freepark.cloud.simple.parking.repository;

import com.freepark.cloud.simple.parking.entity.ParkingLane;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParkingLaneRepository extends JpaRepository<ParkingLane, Long> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<ParkingLane> findAllByOrderByCreatedAtDesc();

    List<ParkingLane> findAllByLot_IdOrLinkedLot_IdOrderByCreatedAtDesc(Long lotId, Long linkedLotId);

    List<ParkingLane> findByLot_IdOrderByCreatedAtAsc(Long lotId);
}

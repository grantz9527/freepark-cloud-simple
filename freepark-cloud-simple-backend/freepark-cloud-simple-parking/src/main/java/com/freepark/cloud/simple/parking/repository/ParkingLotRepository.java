package com.freepark.cloud.simple.parking.repository;

import com.freepark.cloud.simple.parking.entity.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParkingLotRepository extends JpaRepository<ParkingLot, Long> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<ParkingLot> findAllByOrderByCreatedAtDesc();

    Optional<ParkingLot> findByCode(String code);
}

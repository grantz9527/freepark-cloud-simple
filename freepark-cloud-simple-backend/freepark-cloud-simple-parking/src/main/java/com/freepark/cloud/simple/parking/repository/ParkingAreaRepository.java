package com.freepark.cloud.simple.parking.repository;

import com.freepark.cloud.simple.parking.entity.ParkingArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParkingAreaRepository extends JpaRepository<ParkingArea, Long> {

    List<ParkingArea> findByLocationIdOrderByNameAsc(Long locationId);

    List<ParkingArea> findByLocationLotIdOrderByNameAsc(Long lotId);

    boolean existsByLocationIdAndNameIgnoreCase(Long locationId, String name);

    boolean existsByLocationIdAndNameIgnoreCaseAndIdNot(Long locationId, String name, Long id);
}

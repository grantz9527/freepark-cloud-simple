package com.freepark.cloud.simple.parking.repository;

import com.freepark.cloud.simple.parking.entity.ParkingLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParkingLocationRepository extends JpaRepository<ParkingLocation, Long> {

    List<ParkingLocation> findByLotIdOrderByNameAsc(Long lotId);

    boolean existsByLotIdAndNameIgnoreCase(Long lotId, String name);

    boolean existsByLotIdAndNameIgnoreCaseAndIdNot(Long lotId, String name, Long id);
}

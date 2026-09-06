package com.freepark.cloud.simple.parking.repository;

import com.freepark.cloud.simple.parking.entity.ParkingSpace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ParkingSpaceRepository
        extends JpaRepository<ParkingSpace, Long>, JpaSpecificationExecutor<ParkingSpace> {

    boolean existsByLotIdAndCodeIgnoreCase(Long lotId, String code);

    boolean existsByLotIdAndCodeIgnoreCaseAndIdNot(Long lotId, String code, Long id);
}

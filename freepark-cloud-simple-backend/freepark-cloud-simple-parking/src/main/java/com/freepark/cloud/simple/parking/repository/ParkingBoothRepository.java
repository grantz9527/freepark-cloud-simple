package com.freepark.cloud.simple.parking.repository;

import com.freepark.cloud.simple.parking.entity.ParkingBooth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ParkingBoothRepository
        extends JpaRepository<ParkingBooth, Long>, JpaSpecificationExecutor<ParkingBooth> {

    boolean existsByLotIdAndNameIgnoreCase(Long lotId, String name);

    boolean existsByLotIdAndNameIgnoreCaseAndIdNot(Long lotId, String name, Long id);

    boolean existsByLotIdAndCodeIgnoreCase(Long lotId, String code);

    boolean existsByLotIdAndCodeIgnoreCaseAndIdNot(Long lotId, String code, Long id);
}

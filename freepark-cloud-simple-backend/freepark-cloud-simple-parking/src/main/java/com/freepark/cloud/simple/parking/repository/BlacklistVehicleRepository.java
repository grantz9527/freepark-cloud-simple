package com.freepark.cloud.simple.parking.repository;

import com.freepark.cloud.simple.parking.entity.BlacklistVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BlacklistVehicleRepository
        extends JpaRepository<BlacklistVehicle, Long>, JpaSpecificationExecutor<BlacklistVehicle> {

    boolean existsByLotIdAndPlateNumberIgnoreCaseAndEnabledTrue(Long lotId, String plate);

    boolean existsByLotIdAndPlateNumberIgnoreCase(Long lotId, String plate);

    boolean existsByLotIdAndPlateNumberIgnoreCaseAndIdNot(Long lotId, String plate, Long id);
}

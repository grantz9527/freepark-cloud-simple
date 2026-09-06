package com.freepark.cloud.simple.parking.repository;

import com.freepark.cloud.simple.parking.entity.InternalVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface InternalVehicleRepository
        extends JpaRepository<InternalVehicle, Long>, JpaSpecificationExecutor<InternalVehicle> {

    boolean existsByLotIdAndPlateNumberIgnoreCase(Long lotId, String plate);

    boolean existsByLotIdAndPlateNumberIgnoreCaseAndIdNot(Long lotId, String plate, Long id);

    boolean existsByLotIdAndPlateNumberIgnoreCaseAndEnabledTrue(Long lotId, String plate);

    List<InternalVehicle> findByLotIdAndPlateNumberIgnoreCase(Long lotId, String plate);

    List<InternalVehicle> findAllByLotIdAndBatchId(Long lotId, String batchId);
}

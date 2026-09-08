package com.freepark.cloud.simple.parking.repository;

import com.freepark.cloud.simple.parking.entity.InternalVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface InternalVehicleRepository
        extends JpaRepository<InternalVehicle, Long>, JpaSpecificationExecutor<InternalVehicle> {

    /** 车场下全部内部车辆（按创建顺序），用于云端全量快照下发 */
    List<InternalVehicle> findAllByLotIdOrderByIdAsc(Long lotId);

    boolean existsByLotIdAndPlateNumberIgnoreCase(Long lotId, String plate);

    boolean existsByLotIdAndPlateNumberIgnoreCaseAndIdNot(Long lotId, String plate, Long id);

    boolean existsByLotIdAndPlateNumberIgnoreCaseAndEnabledTrue(Long lotId, String plate);

    List<InternalVehicle> findByLotIdAndPlateNumberIgnoreCase(Long lotId, String plate);

    List<InternalVehicle> findAllByLotIdAndBatchId(Long lotId, String batchId);
}

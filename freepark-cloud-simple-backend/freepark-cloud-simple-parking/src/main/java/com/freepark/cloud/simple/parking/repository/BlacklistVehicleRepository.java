package com.freepark.cloud.simple.parking.repository;

import com.freepark.cloud.simple.parking.entity.BlacklistVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface BlacklistVehicleRepository
        extends JpaRepository<BlacklistVehicle, Long>, JpaSpecificationExecutor<BlacklistVehicle> {

    /** 车场下全部黑名单记录（按创建顺序），用于云端全量快照下发 */
    List<BlacklistVehicle> findAllByLotIdOrderByIdAsc(Long lotId);

    boolean existsByLotIdAndPlateNumberIgnoreCaseAndEnabledTrue(Long lotId, String plate);

    boolean existsByLotIdAndPlateNumberIgnoreCase(Long lotId, String plate);

    boolean existsByLotIdAndPlateNumberIgnoreCaseAndIdNot(Long lotId, String plate, Long id);
}

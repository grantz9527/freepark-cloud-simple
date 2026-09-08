package com.freepark.cloud.simple.parking.repository;

import com.freepark.cloud.simple.parking.entity.ParkingSpace;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ParkingSpaceRepository
        extends JpaRepository<ParkingSpace, Long>, JpaSpecificationExecutor<ParkingSpace> {

    /** 车场下全部车位（按创建顺序），预取区域/位置供云端全量快照拼装层级信息 */
    @EntityGraph(attributePaths = {"area", "area.location"})
    List<ParkingSpace> findAllByLotIdOrderByIdAsc(Long lotId);

    boolean existsByLotIdAndCodeIgnoreCase(Long lotId, String code);

    boolean existsByLotIdAndCodeIgnoreCaseAndIdNot(Long lotId, String code, Long id);
}

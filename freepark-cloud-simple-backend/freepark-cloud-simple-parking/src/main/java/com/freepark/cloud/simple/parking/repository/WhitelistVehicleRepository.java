package com.freepark.cloud.simple.parking.repository;

import com.freepark.cloud.simple.parking.entity.WhitelistVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface WhitelistVehicleRepository
        extends JpaRepository<WhitelistVehicle, Long>, JpaSpecificationExecutor<WhitelistVehicle> {

    /**
     * 车场下是否存在当前时间正处于有效时间区间（且启用）的白名单记录。
     * 同一车牌可有多张停车卡（多条记录），只有区间覆盖当前时刻的才视为有效白名单。
     */
    @Query("""
            select count(w) > 0 from WhitelistVehicle w
            where w.lot.id = :lotId
              and lower(w.plateNumber) = lower(:plateNumber)
              and w.enabled = true
              and (w.startTime is null or w.startTime <= :now)
              and (w.endTime is null or w.endTime >= :now)
            """)
    boolean existsActiveAt(@Param("lotId") Long lotId, @Param("plateNumber") String plateNumber,
                          @Param("now") LocalDateTime now);

    /** 车牌在车场内的全部记录（含已过期/未生效，用于编辑校验与列表展示） */
    List<WhitelistVehicle> findAllByLotIdAndPlateNumberIgnoreCase(Long lotId, String plate);
}

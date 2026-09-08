package com.freepark.cloud.simple.parking.repository;

import com.freepark.cloud.simple.parking.entity.DiscountVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface DiscountVehicleRepository
        extends JpaRepository<DiscountVehicle, Long>, JpaSpecificationExecutor<DiscountVehicle> {

    /** 车场下某车牌的优惠记录（唯一），含停用记录，用于编辑/新增查重。 */
    List<DiscountVehicle> findAllByLotIdAndPlateNumberIgnoreCase(Long lotId, String plate);

    /** 车场下某车牌当前启用的优惠记录，用于结算减免。 */
    Optional<DiscountVehicle> findByLotIdAndPlateNumberIgnoreCaseAndEnabledTrue(Long lotId, String plate);
}

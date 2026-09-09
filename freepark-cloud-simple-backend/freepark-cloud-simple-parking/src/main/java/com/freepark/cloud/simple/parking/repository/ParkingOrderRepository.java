package com.freepark.cloud.simple.parking.repository;

import com.freepark.cloud.simple.parking.entity.ParkingOrder;
import com.freepark.cloud.simple.parking.entity.ParkingOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ParkingOrderRepository
        extends JpaRepository<ParkingOrder, Long>, JpaSpecificationExecutor<ParkingOrder> {

    /** 指定流水的未支付（待支付）订单。 */
    List<ParkingOrder> findBySessionIdAndStatus(Long sessionId, ParkingOrderStatus status);

    /** 按关联流水分组汇总指定状态的订单金额；无命中组不在结果中出现。 */
    @Query("select o.sessionId, sum(o.amountYuan) from ParkingOrder o "
            + "where o.sessionId in :sessionIds and o.status = :status "
            + "group by o.sessionId")
    List<Object[]> sumAmountGroupBySessionId(@Param("sessionIds") Collection<Long> sessionIds,
                                             @Param("status") ParkingOrderStatus status);
}

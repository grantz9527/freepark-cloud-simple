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

    /** 指定支付单下处于指定状态的订单（C 端在线支付按流水拆出的订单集合）。 */
    List<ParkingOrder> findByPaymentNoAndStatus(String paymentNo, ParkingOrderStatus status);

    /** 指定支付单下的全部订单（按主键升序），用于缴款单详情展示拆分明细。 */
    List<ParkingOrder> findByPaymentNoOrderByIdAsc(String paymentNo);

    /** 按关联流水分组汇总指定状态的订单金额；无命中组不在结果中出现。 */
    @Query("select o.sessionId, sum(o.amountYuan) from ParkingOrder o "
            + "where o.sessionId in :sessionIds and o.status = :status "
            + "group by o.sessionId")
    List<Object[]> sumAmountGroupBySessionId(@Param("sessionIds") Collection<Long> sessionIds,
                                             @Param("status") ParkingOrderStatus status);

    /**
     * 按关联流水分组汇总「管理端人工下单」的未支付订单金额（不含 C 端在线支付产生的订单）。
     * C 端查费/下单用该口径：在线支付自身的待付订单不应占用车主可见的待缴金额，
     * 否则用户中途放弃支付会让查费结果凭空减少。
     */
    @Query("select o.sessionId, sum(o.amountYuan) from ParkingOrder o "
            + "where o.sessionId in :sessionIds and o.status = :status and o.paymentNo is null "
            + "group by o.sessionId")
    List<Object[]> sumManualAmountGroupBySessionId(@Param("sessionIds") Collection<Long> sessionIds,
                                                   @Param("status") ParkingOrderStatus status);
}

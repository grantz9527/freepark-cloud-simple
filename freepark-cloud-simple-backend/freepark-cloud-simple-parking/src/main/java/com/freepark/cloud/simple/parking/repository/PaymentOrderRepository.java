package com.freepark.cloud.simple.parking.repository;

import com.freepark.cloud.simple.parking.entity.PaymentOrder;
import com.freepark.cloud.simple.parking.entity.PaymentOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    /** 按支付单号查询。 */
    Optional<PaymentOrder> findByPayNo(String payNo);

    /** 指定车牌下处于指定状态的支付单（用于下单前关闭同车牌未完成的支付）。 */
    List<PaymentOrder> findByStatusAndPlateNumber(PaymentOrderStatus status, String plateNumber);
}

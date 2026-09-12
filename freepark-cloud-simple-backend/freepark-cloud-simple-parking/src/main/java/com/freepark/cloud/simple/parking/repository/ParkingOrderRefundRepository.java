package com.freepark.cloud.simple.parking.repository;

import com.freepark.cloud.simple.parking.entity.ParkingOrderRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ParkingOrderRefundRepository
        extends JpaRepository<ParkingOrderRefund, Long>, JpaSpecificationExecutor<ParkingOrderRefund> {

    List<ParkingOrderRefund> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}

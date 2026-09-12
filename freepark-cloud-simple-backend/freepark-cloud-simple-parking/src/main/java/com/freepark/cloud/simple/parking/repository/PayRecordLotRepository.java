package com.freepark.cloud.simple.parking.repository;

import com.freepark.cloud.simple.parking.entity.PayRecordLot;
import com.freepark.cloud.simple.parking.entity.PayRecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface PayRecordLotRepository extends JpaRepository<PayRecordLot, Long> {

    List<PayRecordLot> findByRecordIdInOrderByIdAsc(Collection<Long> recordIds);

    /** 成功的支付/退款请求及其车场金额，按成功时间（无则用创建时间）落在 UTC 区间内。 */
    @Query("select l, r from PayRecordLot l, PayRecord r "
            + "where l.recordId = r.id and r.status = :status "
            + "and coalesce(r.successTime, r.createdAt) >= :start "
            + "and coalesce(r.successTime, r.createdAt) < :end")
    List<Object[]> findLotsWithRecord(@Param("status") PayRecordStatus status,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);
}

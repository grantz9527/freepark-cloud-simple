package com.freepark.cloud.simple.parking.repository;

import com.freepark.cloud.simple.parking.entity.PayRecord;
import com.freepark.cloud.simple.parking.entity.PayRecordKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PayRecordRepository
        extends JpaRepository<PayRecord, Long>, JpaSpecificationExecutor<PayRecord> {

    Optional<PayRecord> findByKindAndRelatedPayNo(PayRecordKind kind, String relatedPayNo);

    Optional<PayRecord> findByRecordNo(String recordNo);
}

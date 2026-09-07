package com.freepark.cloud.simple.billing.repository;

import com.freepark.cloud.simple.billing.entity.BillingCycleSegment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingCycleSegmentRepository extends JpaRepository<BillingCycleSegment, Long> {

    List<BillingCycleSegment> findByProfileIdOrderBySeqAsc(Long profileId);

    void deleteByProfileId(Long profileId);
}

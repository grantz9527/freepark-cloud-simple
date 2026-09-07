package com.freepark.cloud.simple.billing.repository;

import com.freepark.cloud.simple.billing.entity.BillingCycleProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingCycleProfileRepository extends JpaRepository<BillingCycleProfile, Long> {

    List<BillingCycleProfile> findAllByOrderByIdAsc();
}

package com.freepark.cloud.simple.billing.repository;

import com.freepark.cloud.simple.billing.entity.BillingDailyRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingDailyRuleRepository extends JpaRepository<BillingDailyRule, Long> {

    List<BillingDailyRule> findAllByOrderByIdAsc();

    List<BillingDailyRule> findByCycleProfileId(Long profileId);
}

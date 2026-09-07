package com.freepark.cloud.simple.billing.repository;

import com.freepark.cloud.simple.billing.entity.BillingGeneralRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingGeneralRuleRepository extends JpaRepository<BillingGeneralRule, Long> {

    List<BillingGeneralRule> findAllByOrderByIdAsc();

    List<BillingGeneralRule> findByCycleProfileId(Long profileId);
}

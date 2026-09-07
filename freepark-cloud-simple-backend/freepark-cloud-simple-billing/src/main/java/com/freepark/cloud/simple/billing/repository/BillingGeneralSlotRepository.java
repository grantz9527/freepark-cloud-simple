package com.freepark.cloud.simple.billing.repository;

import com.freepark.cloud.simple.billing.entity.BillingGeneralSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingGeneralSlotRepository extends JpaRepository<BillingGeneralSlot, Long> {

    List<BillingGeneralSlot> findByRuleIdOrderByWeekdayAscStartMinuteAscIdAsc(Long ruleId);

    void deleteByRuleId(Long ruleId);
}

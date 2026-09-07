package com.freepark.cloud.simple.billing.repository;

import com.freepark.cloud.simple.billing.entity.BillingDailySlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingDailySlotRepository extends JpaRepository<BillingDailySlot, Long> {

    List<BillingDailySlot> findByRuleIdOrderByWeekdayAscStartMinuteAscIdAsc(Long ruleId);

    void deleteByRuleId(Long ruleId);
}

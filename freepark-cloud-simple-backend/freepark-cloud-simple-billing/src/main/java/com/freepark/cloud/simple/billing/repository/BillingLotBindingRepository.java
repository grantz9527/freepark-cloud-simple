package com.freepark.cloud.simple.billing.repository;

import com.freepark.cloud.simple.billing.entity.BillingLotBinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingLotBindingRepository extends JpaRepository<BillingLotBinding, Long> {

    /** 查询某车场的全部计费配置（含每日制与 24 小时制），用于冲突校验与列表组装 */
    List<BillingLotBinding> findByLotId(Long lotId);

    List<BillingLotBinding> findByLotIdOrderByEffectiveFromAscIdAsc(Long lotId);

    List<BillingLotBinding> findAllByOrderByLotIdAscEffectiveFromAscIdAsc();

    /** 某条规则模板是否被任意车场引用（用于删除前的引用校验） */
    List<BillingLotBinding> findByRuleTypeAndRuleId(String ruleType, Long ruleId);
}

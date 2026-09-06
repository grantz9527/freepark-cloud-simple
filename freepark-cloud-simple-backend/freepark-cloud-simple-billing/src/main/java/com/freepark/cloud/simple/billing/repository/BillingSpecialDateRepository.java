package com.freepark.cloud.simple.billing.repository;

import com.freepark.cloud.simple.billing.entity.BillingSpecialDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface BillingSpecialDateRepository
        extends JpaRepository<BillingSpecialDate, Long>, JpaSpecificationExecutor<BillingSpecialDate> {

    List<BillingSpecialDate> findAllByOrderByStartTimeAscEndTimeAsc();

    /**
     * 是否存在与给定时间区间相交的记录（排除指定 id，供编辑时复用）。
     * 区间为半开区间 [start, end)：结束时间不含。
     */
    default boolean overlapsExcluding(LocalDateTime start, LocalDateTime end, Long excludeId) {
        List<BillingSpecialDate> all = findAllByOrderByStartTimeAscEndTimeAsc();
        for (BillingSpecialDate entry : all) {
            if (entry.getId().equals(excludeId)) {
                continue;
            }
            if (start.isBefore(entry.getEndTime()) && entry.getStartTime().isBefore(end)) {
                return true;
            }
        }
        return false;
    }
}

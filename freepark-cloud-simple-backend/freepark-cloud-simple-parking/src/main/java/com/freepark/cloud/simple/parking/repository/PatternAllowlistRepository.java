package com.freepark.cloud.simple.parking.repository;

import com.freepark.cloud.simple.parking.entity.PatternAllowlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface PatternAllowlistRepository
        extends JpaRepository<PatternAllowlist, Long>, JpaSpecificationExecutor<PatternAllowlist> {

    /** 车场下全部号段放行规则（按创建顺序），用于云端全量快照下发 */
    List<PatternAllowlist> findAllByLotIdOrderByIdAsc(Long lotId);

    List<PatternAllowlist> findByLotIdAndEnabledTrue(Long lotId);

    boolean existsByLotIdAndNameIgnoreCase(Long lotId, String name);

    boolean existsByLotIdAndNameIgnoreCaseAndIdNot(Long lotId, String name, Long id);

    boolean existsByLotIdAndPattern(Long lotId, String pattern);

    boolean existsByLotIdAndPatternAndIdNot(Long lotId, String pattern, Long id);
}

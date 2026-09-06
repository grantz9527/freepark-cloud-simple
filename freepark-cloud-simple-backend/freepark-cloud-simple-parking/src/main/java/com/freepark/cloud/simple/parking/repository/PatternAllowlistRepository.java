package com.freepark.cloud.simple.parking.repository;

import com.freepark.cloud.simple.parking.entity.PatternAllowlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface PatternAllowlistRepository
        extends JpaRepository<PatternAllowlist, Long>, JpaSpecificationExecutor<PatternAllowlist> {

    List<PatternAllowlist> findByLotIdAndEnabledTrue(Long lotId);

    boolean existsByLotIdAndNameIgnoreCase(Long lotId, String name);

    boolean existsByLotIdAndNameIgnoreCaseAndIdNot(Long lotId, String name, Long id);

    boolean existsByLotIdAndPattern(Long lotId, String pattern);

    boolean existsByLotIdAndPatternAndIdNot(Long lotId, String pattern, Long id);
}

package com.freepark.cloud.simple.parking.repository;

import com.freepark.cloud.simple.parking.entity.EdgeNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EdgeNodeRepository extends JpaRepository<EdgeNode, Long> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<EdgeNode> findAllByOrderByCreatedAtDesc();

    Optional<EdgeNode> findByCode(String code);
}

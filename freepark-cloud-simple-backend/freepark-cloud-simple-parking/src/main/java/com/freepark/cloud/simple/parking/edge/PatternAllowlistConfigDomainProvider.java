package com.freepark.cloud.simple.parking.edge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.entity.PatternAllowlist;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.parking.repository.PatternAllowlistRepository;
import com.freepark.cloud.simple.settings.runtime.EdgeConfigDomainSnapshotProvider;
import com.freepark.cloud.simple.settings.runtime.EdgeConfigSyncProtocol;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 正则名单（车牌号段放行规则）域快照构建器：输出车场当前全部放行名单规则，
 * 供云端 v3 全量快照按域分帧下发。
 */
@Component
@Order(20)
public class PatternAllowlistConfigDomainProvider implements EdgeConfigDomainSnapshotProvider {

    private final PatternAllowlistRepository repository;
    private final ParkingLotRepository lotRepository;
    private final ObjectMapper objectMapper;

    public PatternAllowlistConfigDomainProvider(PatternAllowlistRepository repository,
            ParkingLotRepository lotRepository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.lotRepository = lotRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String domain() {
        return EdgeConfigSyncProtocol.DOMAIN_PATTERN;
    }

    @Override
    public String buildDomainItemsJson(String parkCode) {
        ParkingLot lot = lotRepository.findByCode(parkCode).orElse(null);
        if (lot == null) {
            return "[]";
        }
        List<PatternAllowlist> rows = repository.findAllByLotIdOrderByIdAsc(lot.getId());
        try {
            ArrayNode array = objectMapper.createArrayNode();
            for (PatternAllowlist rule : rows) {
                array.add(EdgeDomainItems.pattern(objectMapper, rule));
            }
            return objectMapper.writeValueAsString(array);
        } catch (Exception e) {
            throw new IllegalStateException("serialize pattern allowlist failed: " + e.getMessage(), e);
        }
    }
}

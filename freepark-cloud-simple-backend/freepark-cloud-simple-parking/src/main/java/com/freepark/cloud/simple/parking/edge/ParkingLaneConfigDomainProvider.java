package com.freepark.cloud.simple.parking.edge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.freepark.cloud.simple.parking.entity.ParkingLane;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.repository.ParkingLaneRepository;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.settings.runtime.EdgeConfigDomainSnapshotProvider;
import com.freepark.cloud.simple.settings.runtime.EdgeConfigSyncProtocol;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 通道域快照构建器：输出车场当前全部通道（出入口，含关联对向车场）记录，
 * 供云端 v3 全量快照按域分帧下发（空数组表示该域无记录）。
 * 条目 JSON 形状见 {@link EdgeDomainItems#lane}。
 */
@Component
@Order(60)
public class ParkingLaneConfigDomainProvider implements EdgeConfigDomainSnapshotProvider {

    private final ParkingLaneRepository laneRepository;
    private final ParkingLotRepository lotRepository;
    private final ObjectMapper objectMapper;

    public ParkingLaneConfigDomainProvider(ParkingLaneRepository laneRepository,
            ParkingLotRepository lotRepository, ObjectMapper objectMapper) {
        this.laneRepository = laneRepository;
        this.lotRepository = lotRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String domain() {
        return EdgeConfigSyncProtocol.DOMAIN_LANE;
    }

    @Override
    public String buildDomainItemsJson(String parkCode) {
        ParkingLot lot = lotRepository.findByCode(parkCode).orElse(null);
        if (lot == null) {
            return "[]";
        }
        List<ParkingLane> rows = laneRepository.findByLot_IdOrderByCreatedAtAsc(lot.getId());
        try {
            ArrayNode array = objectMapper.createArrayNode();
            for (ParkingLane lane : rows) {
                array.add(EdgeDomainItems.lane(objectMapper, lane));
            }
            return objectMapper.writeValueAsString(array);
        } catch (Exception e) {
            throw new IllegalStateException("serialize parking lane failed: " + e.getMessage(), e);
        }
    }
}

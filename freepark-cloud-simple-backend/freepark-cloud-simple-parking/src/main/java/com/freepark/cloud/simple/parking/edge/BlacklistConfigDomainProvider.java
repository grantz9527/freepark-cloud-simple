package com.freepark.cloud.simple.parking.edge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.freepark.cloud.simple.parking.entity.BlacklistVehicle;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.repository.BlacklistVehicleRepository;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.settings.runtime.EdgeConfigDomainSnapshotProvider;
import com.freepark.cloud.simple.settings.runtime.EdgeConfigSyncProtocol;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 黑名单域快照构建器：输出车场当前全部黑名单车辆记录，
 * 供云端 v3 全量快照按域分帧下发（空数组表示该域无记录）。
 */
@Component
@Order(10)
public class BlacklistConfigDomainProvider implements EdgeConfigDomainSnapshotProvider {

    private final BlacklistVehicleRepository repository;
    private final ParkingLotRepository lotRepository;
    private final ObjectMapper objectMapper;

    public BlacklistConfigDomainProvider(BlacklistVehicleRepository repository,
            ParkingLotRepository lotRepository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.lotRepository = lotRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String domain() {
        return EdgeConfigSyncProtocol.DOMAIN_BLACKLIST;
    }

    @Override
    public String buildDomainItemsJson(String parkCode) {
        ParkingLot lot = lotRepository.findByCode(parkCode).orElse(null);
        if (lot == null) {
            return "[]";
        }
        List<BlacklistVehicle> rows = repository.findAllByLotIdOrderByIdAsc(lot.getId());
        try {
            ArrayNode array = objectMapper.createArrayNode();
            for (BlacklistVehicle vehicle : rows) {
                array.add(EdgeDomainItems.blacklist(objectMapper, vehicle));
            }
            return objectMapper.writeValueAsString(array);
        } catch (Exception e) {
            throw new IllegalStateException("serialize blacklist failed: " + e.getMessage(), e);
        }
    }
}

package com.freepark.cloud.simple.parking.edge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.freepark.cloud.simple.parking.entity.InternalVehicle;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.repository.InternalVehicleRepository;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.settings.runtime.EdgeConfigDomainSnapshotProvider;
import com.freepark.cloud.simple.settings.runtime.EdgeConfigSyncProtocol;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 内部车辆域快照构建器：输出车场当前全部内部车辆记录。
 * 内部车条目量可能很大（单个车场数千至上万），由调度方按帧上限自动分片。
 */
@Component
@Order(40)
public class InternalVehicleConfigDomainProvider implements EdgeConfigDomainSnapshotProvider {

    private final InternalVehicleRepository repository;
    private final ParkingLotRepository lotRepository;
    private final ObjectMapper objectMapper;

    public InternalVehicleConfigDomainProvider(InternalVehicleRepository repository,
            ParkingLotRepository lotRepository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.lotRepository = lotRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String domain() {
        return EdgeConfigSyncProtocol.DOMAIN_INTERNAL;
    }

    @Override
    public String buildDomainItemsJson(String parkCode) {
        ParkingLot lot = lotRepository.findByCode(parkCode).orElse(null);
        if (lot == null) {
            return "[]";
        }
        List<InternalVehicle> rows = repository.findAllByLotIdOrderByIdAsc(lot.getId());
        try {
            ArrayNode array = objectMapper.createArrayNode();
            for (InternalVehicle vehicle : rows) {
                array.add(EdgeDomainItems.internal(objectMapper, vehicle));
            }
            return objectMapper.writeValueAsString(array);
        } catch (Exception e) {
            throw new IllegalStateException("serialize internal vehicle failed: " + e.getMessage(), e);
        }
    }
}

package com.freepark.cloud.simple.parking.edge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.entity.WhitelistVehicle;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.parking.repository.WhitelistVehicleRepository;
import com.freepark.cloud.simple.settings.runtime.EdgeConfigDomainSnapshotProvider;
import com.freepark.cloud.simple.settings.runtime.EdgeConfigSyncProtocol;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 白名单（停车卡）域快照构建器：输出车场当前全部白名单记录。
 * 白名单条目量可能很大（单个车场数千至上万），由调度方按帧上限自动分片；
 * 同一车牌可有多条带时间区间的停车卡记录，全部原样下发，由边缘侧本地判定生效。
 */
@Component
@Order(30)
public class WhitelistConfigDomainProvider implements EdgeConfigDomainSnapshotProvider {

    private final WhitelistVehicleRepository repository;
    private final ParkingLotRepository lotRepository;
    private final ObjectMapper objectMapper;

    public WhitelistConfigDomainProvider(WhitelistVehicleRepository repository,
            ParkingLotRepository lotRepository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.lotRepository = lotRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String domain() {
        return EdgeConfigSyncProtocol.DOMAIN_WHITELIST;
    }

    @Override
    public String buildDomainItemsJson(String parkCode) {
        ParkingLot lot = lotRepository.findByCode(parkCode).orElse(null);
        if (lot == null) {
            return "[]";
        }
        List<WhitelistVehicle> rows = repository.findAllByLotIdOrderByIdAsc(lot.getId());
        try {
            ArrayNode array = objectMapper.createArrayNode();
            for (WhitelistVehicle vehicle : rows) {
                array.add(EdgeDomainItems.whitelist(objectMapper, vehicle));
            }
            return objectMapper.writeValueAsString(array);
        } catch (Exception e) {
            throw new IllegalStateException("serialize whitelist failed: " + e.getMessage(), e);
        }
    }
}

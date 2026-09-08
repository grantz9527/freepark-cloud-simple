package com.freepark.cloud.simple.parking.edge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.freepark.cloud.simple.parking.entity.ParkingArea;
import com.freepark.cloud.simple.parking.entity.ParkingLocation;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.entity.ParkingSpace;
import com.freepark.cloud.simple.parking.repository.ParkingAreaRepository;
import com.freepark.cloud.simple.parking.repository.ParkingLocationRepository;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.parking.repository.ParkingSpaceRepository;
import com.freepark.cloud.simple.settings.runtime.EdgeConfigDomainSnapshotProvider;
import com.freepark.cloud.simple.settings.runtime.EdgeConfigSyncProtocol;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 车位管理域快照构建器：输出车场的三层车位结构（位置 location → 区域 area → 车位 space），
 * 以“type 区分行类型”的扁平条目流下发，顺序保证父行先于子行（location → area → space），
 * 边缘侧整批替换时可按条目顺序重建层级。车位条目量可能很大，由调度方按帧上限自动分片。
 * 条目 JSON 形状见 {@link EdgeDomainItems}（location/area/space）。
 */
@Component
@Order(50)
public class ParkingSpaceConfigDomainProvider implements EdgeConfigDomainSnapshotProvider {

    private final ParkingLotRepository lotRepository;
    private final ParkingLocationRepository locationRepository;
    private final ParkingAreaRepository areaRepository;
    private final ParkingSpaceRepository spaceRepository;
    private final ObjectMapper objectMapper;

    public ParkingSpaceConfigDomainProvider(ParkingLotRepository lotRepository,
            ParkingLocationRepository locationRepository,
            ParkingAreaRepository areaRepository,
            ParkingSpaceRepository spaceRepository,
            ObjectMapper objectMapper) {
        this.lotRepository = lotRepository;
        this.locationRepository = locationRepository;
        this.areaRepository = areaRepository;
        this.spaceRepository = spaceRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String domain() {
        return EdgeConfigSyncProtocol.DOMAIN_SPACE;
    }

    @Override
    public String buildDomainItemsJson(String parkCode) {
        ParkingLot lot = lotRepository.findByCode(parkCode).orElse(null);
        if (lot == null) {
            return "[]";
        }
        Long lotId = lot.getId();
        List<ParkingLocation> locations = locationRepository.findByLotIdOrderByNameAsc(lotId);
        List<ParkingArea> areas = areaRepository.findByLocationLotIdOrderByNameAsc(lotId);
        List<ParkingSpace> spaces = spaceRepository.findAllByLotIdOrderByIdAsc(lotId);
        try {
            ArrayNode array = objectMapper.createArrayNode();
            for (ParkingLocation location : locations) {
                array.add(EdgeDomainItems.location(objectMapper, location));
            }
            for (ParkingArea area : areas) {
                array.add(EdgeDomainItems.area(objectMapper, area));
            }
            for (ParkingSpace space : spaces) {
                array.add(EdgeDomainItems.space(objectMapper, space));
            }
            return objectMapper.writeValueAsString(array);
        } catch (Exception e) {
            throw new IllegalStateException("serialize parking space failed: " + e.getMessage(), e);
        }
    }
}

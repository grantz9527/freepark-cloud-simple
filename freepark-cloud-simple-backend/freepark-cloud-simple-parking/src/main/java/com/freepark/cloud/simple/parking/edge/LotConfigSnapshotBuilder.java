package com.freepark.cloud.simple.parking.edge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.settings.runtime.EdgeConfigSnapshotBuilder;
import org.springframework.stereotype.Component;

/**
 * 车场自身配置（V1 数据片段）：车场基础信息、通行拦截开关与生效的通行判定顺序。
 * 车场业务模块提供给 settings 边缘配置下发的 SPI 实现，供云端周期把节点名下各车场
 * 聚合为 data.lots 下发给对应边缘节点，边缘侧据此同步本地车场配置。
 * 条目 JSON 形状见 {@link EdgeDomainItems#lot}。
 */
@Component
public class LotConfigSnapshotBuilder implements EdgeConfigSnapshotBuilder {

    private final ParkingLotRepository lotRepository;
    private final ObjectMapper objectMapper;

    public LotConfigSnapshotBuilder(ParkingLotRepository lotRepository, ObjectMapper objectMapper) {
        this.lotRepository = lotRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String buildConfigDataJson(String parkCode) {
        ParkingLot lot = lotRepository.findByCode(parkCode).orElse(null);
        if (lot == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(EdgeDomainItems.lot(objectMapper, lot));
        } catch (Exception e) {
            throw new IllegalStateException(
                    "serialize lot config failed: " + e.getMessage(), e);
        }
    }
}

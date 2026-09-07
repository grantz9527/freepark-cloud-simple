package com.freepark.cloud.simple.edge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freepark.cloud.simple.parking.entity.AccessJudgmentRuleType;
import com.freepark.cloud.simple.parking.entity.ParkingLot;
import com.freepark.cloud.simple.parking.repository.ParkingLotRepository;
import com.freepark.cloud.simple.settings.runtime.EdgeConfigSnapshotBuilder;
import org.springframework.stereotype.Component;

/**
 * 车场自身配置（V1 数据片段）：车场基础信息、通行拦截开关与生效的通行判定顺序。
 * 供云端周期下发给对应边缘服务，边缘侧据此同步本地车场配置。
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
            ObjectNode node = objectMapper.createObjectNode();
            node.put("code", lot.getCode());
            node.put("name", lot.getName());
            node.put("lotType", lot.getLotType().name());
            node.put("enabled", lot.isEnabled());
            node.put("entryInterceptArrears", lot.isEntryInterceptArrears());
            node.put("entryInterceptBlacklist", lot.isEntryInterceptBlacklist());
            node.put("exitInterceptArrears", lot.isExitInterceptArrears());
            node.put("exitInterceptBlacklist", lot.isExitInterceptBlacklist());
            ArrayNode judgmentOrder = node.putArray("judgmentOrder");
            for (AccessJudgmentRuleType rule : lot.effectiveJudgmentOrder()) {
                judgmentOrder.add(rule.name());
            }
            node.put("updatedAt", lot.getUpdatedAt().toString());
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "serialize lot config failed: " + e.getMessage(), e);
        }
    }
}
